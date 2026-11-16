async function loadRequest() {
  const requestId = new URLSearchParams(location.search).get('request_id');
  const message = document.querySelector('#message');
  try {
    const response = await fetch(`/request?request_id=${encodeURIComponent(requestId || '')}`);
    const request = await response.json();
    if (!response.ok) throw new Error(request.error);
    document.querySelector('#request-id').value = requestId;
    document.querySelector('#client-id').textContent = request.client_id;
    const scopes = document.querySelector('#scopes');
    if (request.scope.length === 0) {
      const note = document.createElement('p');
      note.textContent = 'No scopes were requested.';
      scopes.append(note);
    }
    for (const scope of request.scope) {
      const label = document.createElement('label');
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.name = `scope_${scope}`;
      checkbox.value = 'yes';
      checkbox.checked = true;
      label.append(checkbox, document.createTextNode(` ${scope}`));
      scopes.append(label);
    }
    document.querySelectorAll('button').forEach(button => { button.disabled = false; });
  } catch (error) {
    message.textContent = error.message;
  }
}
loadRequest();
