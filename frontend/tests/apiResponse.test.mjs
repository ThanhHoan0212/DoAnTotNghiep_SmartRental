import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

const source = readFileSync(new URL('../src/services/apiResponse.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
});
const { parseApiResponse, ApiError } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);

test('CORS text response preserves HTTP status and provides an actionable error', async () => {
  await assert.rejects(parseApiResponse(new Response('Invalid CORS request', { status: 403 })),
    error => error instanceof ApiError && error.status === 403 && error.message.includes('CORS'));
});

test('HTML, empty and malformed responses do not leak JSON syntax errors', async () => {
  for (const body of ['<html>Bad Gateway</html>', '', '{bad', 'null']) {
    await assert.rejects(parseApiResponse(new Response(body, { status: 502 })),
      error => error instanceof ApiError && error.status === 502);
  }
});

test('JSON authentication errors preserve the server message', async () => {
  await assert.rejects(parseApiResponse(Response.json({ success: false, message: 'Sai mật khẩu' }, { status: 401 })),
    error => error instanceof ApiError && error.message === 'Sai mật khẩu' && error.status === 401);
});

test('successful API responses retain their payload', async () => {
  const body = { success: true, data: { accessToken: 'test-token' } };
  assert.deepEqual(await parseApiResponse(Response.json(body)), body);
});
