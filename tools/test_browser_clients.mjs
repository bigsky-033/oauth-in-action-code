// Optional developer check: Node is used to test browser logic, never to run Java exercises.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const sources = [
  ...['ch-6-ex-1', 'ch-6-ex-4', 'ch-8-ex-2'].map(module => {
    const namespace = module.replace(/ch-(\d+)-ex-(\d+)/, (_, ch, ex) => `ch${ch.padStart(2, '0')}ex${ex.padStart(2, '0')}`);
    return `exercises/${module}/src/main/resources/web/${namespace}/client/client.js`;
  }),
  'example/src/main/resources/web/examplemain/implicit/client.js',
  'example/chapter8/src/main/resources/web/examplechapter8/implicit/client.js',
  'example/chapter10/src/main/resources/web/examplechapter10/implicit/client.js'
];
for (const module of sources) {
  const source = fs.readFileSync(`${root}/${module}`, 'utf8');
  const storage = new Map();
  const elements = new Map();
  const context = vm.createContext({
    URLSearchParams, Uint8Array,
    location: { hash: '', search: '', origin: 'http://localhost:9000' },
    sessionStorage: { getItem: key => storage.get(key) ?? null, setItem: (key, value) => storage.set(key, value), removeItem: key => storage.delete(key) },
    document: { querySelector: selector => {
      if (!elements.has(selector)) elements.set(selector, { textContent: '', addEventListener() {} });
      return elements.get(selector);
    } }
  });
  vm.runInContext(source, context);
  const valid = '#access_token=opaque%2Btoken&token_type=Bearer&scope=foo+bar&state=expected';
  assert.equal(context.parseCallback(valid, 'expected').access_token, 'opaque+token');
  assert.equal(context.parseCallback(valid, 'expected').scope, 'foo bar');
  assert.throws(() => context.parseCallback(valid, 'wrong'), /State/);
  assert.throws(() => context.parseCallback(valid, null), /State/);
  assert.throws(() => context.parseCallback('#state=expected&error=access_denied', 'expected'), /access_denied/);
  assert.throws(() => context.parseCallback('#state=expected&token_type=Bearer', 'expected'), /Invalid token/);
  assert.throws(() => context.parseCallback('#state=expected&access_token=token&token_type=Basic', 'expected'), /Invalid token/);
  console.log(`PASS ${module}: browser fragment parsing, encoding, state, error and token validation`);
}
