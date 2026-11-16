"""Root-wide study-module, source, launcher and static-asset invariants."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


inventory = dict(re.findall(r"^\| ((?:exercises|example|class)(?:/[\w\-]+)*) \| ([\w\-]+) \|$",
                            (ROOT / "MIGRATION.md").read_text(), re.M))
for path in (ROOT / "exercises").iterdir():
    if path.is_dir():
        require(str(path.relative_to(ROOT)) in inventory, f"Missing inventory row: {path.name}")
for path in inventory:
    require((ROOT / path).is_dir(), f"Inventory points to missing directory: {path}")

settings = (ROOT / "settings.gradle").read_text()
registered = set(re.findall(r"^include '([^']+)'", settings, re.M))
modules = {str(path.parent.relative_to(ROOT)).replace("/", ":")
           for path in ROOT.rglob("build.gradle")
           if path.parent != ROOT and "build" not in path.parts and ".gradle" not in path.parts}
require(registered == modules, f"Module registration differs from build files: {registered ^ modules}")

packages = {}
resource_namespaces = {}
for module in sorted(modules):
    path = ROOT / module.replace(":", "/")
    relative = str(path.relative_to(ROOT))
    require(inventory.get(relative) in {"in-progress", "verified"}, f"Missing active inventory status: {module}")
    build = (path / "build.gradle").read_text()
    main = re.search(r"mainClass = '([^']+)'", build)
    require(main is not None, f"Missing application main class: {module}")
    if main:
        relative_class = main[1].replace(".", "/") + ".java"
        require(any((path / source / relative_class).is_file() for source in ("src/main/java", "completed/src/main/java")), f"Configured main class missing: {module}: {main[1]}")
    require(f"`:{module}:run`" in (ROOT / "docs/RUNNING.md").read_text(), f"Run index missing module: {module}")
    require((path / "README.md").exists(), f"Missing run instructions: {module}")
    require(any((path / "src/test").rglob("*Test.java")), f"Missing tests: {module}")
    require(module == "class:snippets" or any((path / "src/main/resources").rglob("index.html")),  f"Missing static HTML: {module}")
    for namespace in (path / "src/main/resources/web").glob("*"):
        require(namespace.name not in resource_namespaces,
                f"Static resource namespace reused: {namespace.name}")
        resource_namespaces[namespace.name] = module
    java_files = list(path.glob("src/main/java/**/*.java")) + list(path.glob("completed/src/main/java/**/*.java"))
    require(any("static void main(" in source.read_text() for source in java_files), f"Missing launcher: {module}")
    chapter = re.fullmatch(r"ch-(\d+)-ex-(\d+)", path.name)
    launcher = path / "src/main/java" / ("org/oauthinaction/ch" + chapter[1].zfill(2) + "ex" + chapter[2].zfill(2)) / "Exercise.java" if chapter else None
    if launcher and launcher.exists():
        require(f"Chapter {chapter[1]} exercise {chapter[2]}:" in launcher.read_text(), f"Wrong chapter in launch message: {module}")
    for source in java_files:
        content = source.read_text()
        match = re.search(r"^package ([\w.]+);", content, re.M)
        require(match is not None, f"Missing package: {source}")
        if match:
            package = match.group(1)
            require(package not in packages or packages[package] == module,
                    f"Java package shared between exercises: {package}")
            packages[package] = module
            require(str(source).endswith(package.replace(".", "/") + "/" + source.name),
                    f"Package directory mismatch: {source}")
        require("org.springframework.security" not in content, f"Spring Security import: {source}")
    for page in (path / "src/main/resources").rglob("*.html"):
        content = page.read_text()
        require("<%" not in content and "th:" not in content, f"Template syntax in plain HTML: {page}")
        for link in re.findall(r'(?:src|href)="([^"#?]+)"', content):
            if ":" not in link and not link.startswith("/"):
                require((page.parent / link).is_file(), f"Broken local asset reference: {page}: {link}")
            if link.startswith("/") and link.endswith((".css", ".js")):
                require((page.parent / link.lstrip("/")).is_file(), f"Broken root asset reference: {page}: {link}")
        ids = set(re.findall(r'id="([^"]+)"', content))
        for link in re.findall(r'<script[^>]+src="([^"]+)"', content):
            script = page.parent / link.lstrip("/")
            if script.is_file():
                for selector in re.findall(r"querySelector\(['\"]#([\w-]+)['\"]\)", script.read_text()):
                    require(selector in ids, f"Missing element #{selector}: {page}")
    styles = list((path / "src/main/resources/web").rglob("style.css"))
    require(len({style.read_text() for style in styles}) <= 1, f"Role styles diverged: {module}")

for build in [ROOT / "build.gradle"] + [ROOT / module.replace(":", "/") / "build.gradle" for module in modules]:
    content = build.read_text().lower()
    for prohibited in ("spring-security", "starter-security", "thymeleaf", "freemarker", "lombok"):
        require(prohibited not in content, f"Prohibited dependency {prohibited}: {build}")

for path, status in inventory.items():
    require(status in {"pending", "in-progress", "verified"}, f"Invalid status for {path}: {status}")
    if status == "verified":
        require(path.replace("/", ":") in modules, f"Verified item has no module: {path}")

# Superseded server JS/Node manifests must not quietly re-enter the Java study tree.
for directory in (ROOT / "class", ROOT / "example", ROOT / "exercises"):
    for source in directory.rglob("*"):
        if not source.is_file() or "build" in source.parts or ".gradle" in source.parts:
            continue
        require(source.name != "package.json", f"Legacy Node manifest: {source}")
        if source.suffix == ".js":
            require("src/main/resources/" in source.as_posix(), f"Server JavaScript outside browser resources: {source}")

if errors:
    print("Repository consistency failed:\n" + "\n".join(f"- {error}" for error in errors))
    sys.exit(1)
print(f"Repository consistency OK: {len(inventory)} inventory entries, {len(modules)} Java modules.")
