async function loadRegistration() {
  const output = document.querySelector('#registration');
  try {
    const response = await fetch('/status');
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    output.textContent = JSON.stringify(await response.json(), null, 2);
  } catch (error) {
    output.textContent = error.message;
  }
}
loadRegistration();
