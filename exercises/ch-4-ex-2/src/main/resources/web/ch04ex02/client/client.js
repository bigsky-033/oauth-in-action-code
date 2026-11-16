const message = document.querySelector('#message');

async function readJson(path) {
  const response = await fetch(path);
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || `HTTP ${response.status}`);
  return data;
}

async function loadStatus() {
  try {
    const status = await readJson('/status');
    document.querySelector('#variant').textContent = status.starter
      ? 'Starter resource server'
      : 'Completed solution';
    document.querySelector('#access-token').textContent = status.access_token || 'NONE';
    document.querySelector('#refresh-token').textContent = status.refresh_token || 'NONE';
    document.querySelector('#scope').textContent = status.scope || 'NONE';
  } catch (error) {
    message.textContent = error.message;
  }
}

for (const button of document.querySelectorAll('[data-route]')) {
  button.addEventListener('click', async () => {
    let route = button.dataset.route;
    if (route === '/add_word') route += `?word=${encodeURIComponent(document.querySelector('#word').value)}`;
    try {
      const response = await fetch(route);
      const text = await response.text();
      const data = text ? JSON.parse(text) : null;
      if (!response.ok) throw new Error(data?.error || `HTTP ${response.status}`);
      message.textContent = `HTTP ${response.status}`;
      document.querySelector('#resource').textContent = data ? JSON.stringify(data, null, 2) : '';
    } catch (error) { message.textContent = error.message; }
    await loadStatus();
  });
}

message.textContent = new URLSearchParams(location.search).get('error') || '';
loadStatus();
