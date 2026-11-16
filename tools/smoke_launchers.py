"""Opt-in launch check. Requires free localhost ports 9000–9002 and installDist."""
from contextlib import ExitStack
from http.cookiejar import CookieJar
from pathlib import Path
import subprocess
import tempfile
import time
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, urlencode, urljoin, urlsplit
from urllib.request import build_opener, HTTPCookieProcessor, HTTPRedirectHandler, Request
import json

ROOT = Path(__file__).resolve().parents[1]


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        return None


def stop(process):
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)


def check(exercise, package, separate):
    distribution = ROOT / "exercises" / exercise / "build/install" / exercise
    if not distribution.is_dir():
        raise RuntimeError(f"Build :exercises:{exercise}:installDist first")

    # Refuse to disturb an existing exercise or unrelated local server.
    import socket
    for port in (9000, 9001, 9002):
        with socket.socket() as probe:
            if probe.connect_ex(("localhost", port)) == 0:
                raise RuntimeError(f"Port {port} is occupied; stop the existing server first")

    with tempfile.TemporaryDirectory(prefix="oauth-launch-") as cwd, ExitStack() as stack:
        if separate:
            classes = ["AuthorizationServer", "ProtectedResource", "completed.CompletedClient"]
            commands = [["java", "-cp", str(distribution / "lib/*"), f"{package}.{name}"] for name in classes]
        else:
            commands = [[str(distribution / "bin" / exercise)]]
        processes = []
        for index, command in enumerate(commands):
            log = stack.enter_context(open(Path(cwd) / f"server-{index}.log", "w+"))
            process = subprocess.Popen(command, cwd=cwd, stdout=log, stderr=subprocess.STDOUT)
            stack.callback(stop, process)
            processes.append((process, log))

        browser = build_opener(HTTPCookieProcessor(CookieJar()), NoRedirect())

        def request(url, form=None):
            data = None if form is None else urlencode(form).encode()
            try:
                response = browser.open(Request(url, data=data), timeout=5)
            except HTTPError as response_error:
                response = response_error
            with response:
                return response.code, response.headers, response.read().decode()

        deadline = time.monotonic() + 30
        for port in (9000, 9001, 9002):
            while True:
                for process, log in processes:
                    if process.poll() is not None:
                        log.seek(0)
                        raise RuntimeError(f"Launcher exited: {log.read()}")
                try:
                    status, _, page = request(f"http://localhost:{port}/")
                    assert status == 200 and "<!doctype html>" in page
                    break
                except (URLError, ConnectionError, TimeoutError):
                    if time.monotonic() >= deadline:
                        raise RuntimeError(f"Server on {port} did not start")
                    time.sleep(0.1)

        if exercise == "ch-3-ex-2":
            status, headers, _ = request("http://localhost:9000/fetch_resource")
            assert status == 302
            status, _, body = request(urljoin("http://localhost:9000/", headers["Location"]))
            assert status == 200 and json.loads(body)["name"] == "Protected Resource"

        status, headers, _ = request("http://localhost:9000/authorize")
        assert status == 302
        status, headers, _ = request(headers["Location"])
        assert status == 302
        request_id = parse_qs(urlsplit(headers["Location"]).query)["request_id"][0]
        status, headers, _ = request("http://localhost:9001/approve", {
            "request_id": request_id, "approve": "yes"
        })
        assert status == 302
        status, _, _ = request(headers["Location"])
        assert status == 302
        status, _, body = request("http://localhost:9000/fetch_resource")
        assert status == 200 and json.loads(body)["name"] == "Protected Resource"
        print(f"PASS {exercise}: {'separate JVMs' if separate else 'combined launcher'}, alternate cwd, full code flow")



def require_free_ports():
    import socket
    for port in (9000, 9001, 9002):
        with socket.socket() as probe:
            if probe.connect_ex(("localhost", port)) == 0:
                raise RuntimeError(f"Port {port} is occupied; stop the existing server first")


def launch_case(module, classes=None, console=False):
    """Use installed jars/scripts, outside the project cwd, with no Gradle runtime."""
    import re
    require_free_ports()
    name = module.name
    distribution = module / "build/install" / name
    if not distribution.is_dir():
        raise RuntimeError(f"Build {module.relative_to(ROOT)}:installDist first")
    commands = ([["java", "-cp", str(distribution / "lib/*"), main] for main in classes]
                if classes else [[str(distribution / "bin" / name)]])
    label = str(module.relative_to(ROOT)) + (" / " + ", ".join(c.rsplit(".", 1)[-1] for c in classes) if classes else " / default")
    with tempfile.TemporaryDirectory(prefix="oauth-launch-") as cwd, ExitStack() as stack:
        processes = []
        for index, command in enumerate(commands):
            log = stack.enter_context(open(Path(cwd) / f"server-{index}.log", "w+"))
            process = subprocess.Popen(command, cwd=cwd, stdout=log, stderr=subprocess.STDOUT)
            stack.callback(stop, process)
            processes.append((process, log))
        if console:
            process, log = processes[0]
            assert process.wait(timeout=30) == 0
            log.seek(0)
            assert "Validated ID claims:" in log.read()
        else:
            browser = build_opener(NoRedirect())
            deadline = time.monotonic() + 35
            for port in (9000, 9001, 9002):
                while True:
                    for process, log in processes:
                        if process.poll() is not None:
                            log.seek(0)
                            raise RuntimeError(f"{label}: launcher exited: {log.read()}")
                    try:
                        with browser.open(f"http://localhost:{port}/", timeout=3) as response:
                            page = response.read().decode()
                            assert response.code == 200 and "<!doctype html>" in page.lower(), label
                        # Resolve assets exactly as a browser does; catch accidental missing namespaces.
                        for asset in re.findall(r'(?:src|href)="([^"#?]+)"', page):
                            if asset.endswith((".css", ".js")):
                                with browser.open(urljoin(f"http://localhost:{port}/", asset), timeout=3) as response:
                                    assert response.code == 200 and response.read(), (label, asset)
                        break
                    except (URLError, ConnectionError, TimeoutError):
                        if time.monotonic() >= deadline:
                            for _, log in processes:
                                log.seek(0)
                                print(log.read(), flush=True)
                            raise RuntimeError(f"{label}: server on {port} did not start")
                        time.sleep(0.1)
        print(f"PASS {label}: installed classpath, alternate cwd, " + ("console example" if console else "three pages and local assets"), flush=True)


def all_cases():
    import re
    count = 0
    modules = sorted({ROOT / module.replace(":", "/")
                      for module in re.findall(r"^include '([^']+)'", (ROOT / "settings.gradle").read_text(), re.M)})
    for module in modules:
        launch_case(module, console=module == ROOT / "class/snippets")
        count += 1
        starters = list(module.glob("src/main/java/**/StarterExercise.java"))
        for source in starters:
            package = re.search(r"^package ([\w.]+);", source.read_text(), re.M)[1]
            launch_case(module, [package + ".StarterExercise"])
            count += 1
    # Alternate resource variants and browser-only implicit clients have their own mains.
    baseline = "org.oauthinaction.examplechapter8."
    for variant in ("ProtectedResource1", "ProtectedResource2", "ProtectedResource3"):
        launch_case(ROOT / "example/chapter8", [baseline + "AuthorizationServer", baseline + variant, baseline + "Client"])
        count += 1
    for variant in ("ProtectedResource0a", "ProtectedResource0b"):
        launch_case(ROOT / "example/chapter8/solution0", [baseline + "AuthorizationServer", "org.oauthinaction.examplesolution0." + variant, baseline + "Client"])
        count += 1
    for path, package in [("example", "examplemain"), ("example/chapter8", "examplechapter8"), ("example/chapter10", "examplechapter10")]:
        prefix = "org.oauthinaction." + package + "."
        resource = "ProtectedResource0" if path.endswith("chapter8") else "ProtectedResource"
        launch_case(ROOT / path, [prefix + "AuthorizationServer", prefix + resource, prefix + "ImplicitClient"])
        count += 1
    # Actual OAuth across three JVMs verifies the shared-file handoff, plus refresh in exercise 2.
    for exercise, package in [("ch-3-ex-1", "org.oauthinaction.ch03ex01"), ("ch-3-ex-2", "org.oauthinaction.ch03ex02")]:
        check(exercise, package, True)
        count += 1
    print(f"PASS all {count} packaged launch cases", flush=True)


if __name__ == "__main__":
    all_cases()
