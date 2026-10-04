import { test } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { createServer } from 'vite';

test('local API proxy preserves matching Host and Origin on alternate ports', async () => {
  const backend = http.createServer((request, response) => {
    const sameOrigin = request.headers.origin === `http://${request.headers.host}`;
    response.writeHead(sameOrigin ? 200 : 403, { 'Content-Type': 'application/json' });
    response.end(JSON.stringify({ success: sameOrigin }));
  });
  await new Promise(resolve => backend.listen(0, '127.0.0.1', resolve));
  let vite;
  try {
    vite = await createServer({
      configFile: 'vite.config.ts',
      server: {
        host: '127.0.0.1', port: 0,
        proxy: { '/api': { target: `http://127.0.0.1:${backend.address().port}` } },
      },
    });
    await vite.listen();
    const origin = `http://127.0.0.1:${vite.httpServer.address().port}`;
    const response = await fetch(`${origin}/api/v1/auth/login`, {
      method: 'POST', headers: { Origin: origin, 'Content-Type': 'application/json' }, body: '{}',
    });
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { success: true });
  } finally {
    await vite?.close();
    await new Promise(resolve => backend.close(resolve));
  }
});
