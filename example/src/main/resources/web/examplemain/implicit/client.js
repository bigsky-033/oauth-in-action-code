// Fragments never reach Java: the browser must parse and validate this response.
function parseCallback(fragment, expectedState) {
  const values = new URLSearchParams(fragment.replace(/^#/, ''));
  if (!expectedState || values.get('state') !== expectedState) throw new Error('State value did not match');
  if (values.has('error')) throw new Error(values.get('error'));
  if (!values.get('access_token') || values.get('token_type') !== 'Bearer') throw new Error('Invalid token response');
  return { access_token: values.get('access_token'), scope: values.get('scope') || '' };
}

const storageKey = 'examplemain-implicit-token';
const stateKey = 'examplemain-implicit-state';
const message = document.querySelector('#message');
let token = JSON.parse(sessionStorage.getItem(storageKey) || 'null');
if (location.hash) {
  try {
    const state = sessionStorage.getItem(stateKey);
    sessionStorage.removeItem(stateKey);
    token = parseCallback(location.hash, state);
    sessionStorage.setItem(storageKey, JSON.stringify(token));
  } catch (error) {
    token = null;
    sessionStorage.removeItem(storageKey);
    message.textContent = error.message;
  }
  history.replaceState(null, '', '/');
}
// Some early book servers return denial in the query instead of the fragment.
if (location.search) {
  const values = new URLSearchParams(location.search);
  const expected = sessionStorage.getItem(stateKey);
  sessionStorage.removeItem(stateKey);
  message.textContent = expected && expected === values.get('state') ? values.get('error') || '' : 'State value did not match';
  history.replaceState(null, '', '/');
}
function display() {
  document.querySelector('#access-token').textContent = token?.access_token || 'NONE';
  document.querySelector('#scope').textContent = token?.scope || 'NONE';
}
document.querySelector('#authorize').addEventListener('click', () => {
  const bytes = crypto.getRandomValues(new Uint8Array(24));
  const state = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('');
  sessionStorage.setItem(stateKey, state);
  sessionStorage.removeItem(storageKey);
  const parameters = new URLSearchParams({ response_type: 'token', client_id: 'oauth-client-1',
    redirect_uri: `${location.origin}/callback`, scope: 'foo bar', state });
  location.href = `http://localhost:9001/authorize?${parameters}`;
});
document.querySelector('#fetch-resource').addEventListener('click', async () => {
  try {
    if (!token) throw new Error('Missing access token');
    const response = await fetch('http://localhost:9002/resource', {
      method: 'POST', headers: { Authorization: `Bearer ${token.access_token}` }
    });
    if (!response.ok) throw new Error(`Resource returned HTTP ${response.status}`);
    document.querySelector('#resource').textContent = JSON.stringify(await response.json(), null, 2);
    message.textContent = '';
  } catch (error) { message.textContent = error.message; }
});
display();
