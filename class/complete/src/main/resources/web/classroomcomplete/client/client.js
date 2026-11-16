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
      ? 'Starter authorization server'
      : 'Completed solution';
    document.querySelector('#access-token').textContent = status.access_token || 'NONE';
    document.querySelector('#refresh-token').textContent = status.refresh_token || 'NONE';
    document.querySelector('#id-token').textContent = status.id_token ? JSON.stringify(status.id_token, null, 2) : 'NONE';
    document.querySelector('#scope').textContent = status.scope || 'NONE';
  } catch (error) {
    message.textContent = error.message;
  }
}

document.querySelector('#fetch-resource').addEventListener('click', async () => {
  message.textContent = '';
  document.querySelector('#resource').textContent = '';
  try {
    const data = await readJson('/fetch_resource');
    document.querySelector('#resource').textContent = JSON.stringify(data, null, 2);
  } catch (error) {
    message.textContent = error.message;
  }
  await loadStatus();
});

message.textContent = new URLSearchParams(location.search).get('error') || '';
loadStatus();

document.querySelector('#userinfo').addEventListener('click', async () => {
  try { document.querySelector('#resource').textContent = JSON.stringify(await readJson('/userinfo'), null, 2); }
  catch (error) { message.textContent = error.message; }
});
