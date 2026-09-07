import { test } from 'node:test';
import assert from 'node:assert/strict';
import { apiFetch } from '../src/api/apiClient.js';

test('failed bootstrap can recover; unsafe requests never run without a token', async (t) => {
    const calls = [];
    let fail = true;
    t.mock.method(globalThis, 'fetch', async (url, options) => {
        calls.push({ url, options });
        if (url.endsWith('/csrf')) return new Response(JSON.stringify({ token: 'test-token' }), { status: fail ? 401 : 200 });
        return new Response('{}');
    });
    await assert.rejects(apiFetch('http://localhost:8080/api/accounts', { method: 'POST' }), /CSRF/);
    assert.equal(calls.length, 1);
    fail = false;
    await apiFetch('http://localhost:8080/api/accounts', { method: 'post', credentials: 'omit', headers: { 'Content-Type': 'application/json' } });
    assert.equal(calls.length, 3);
    assert.equal(calls[2].options.credentials, 'include');
    assert.equal(calls[2].options.headers.get('X-XSRF-TOKEN'), 'test-token');
    assert.equal(calls[2].options.headers.get('Content-Type'), 'application/json');
});

test('403 is returned without replaying a mutation', async (t) => {
    let mutations = 0;
    t.mock.method(globalThis, 'fetch', async (url) => {
        if (url.endsWith('/csrf')) return new Response(JSON.stringify({ token: 'token' }));
        mutations++;
        return new Response('{}', { status: 403 });
    });
    const response = await apiFetch('http://localhost:8080/api/payments', { method: 'POST' });
    assert.equal(response.status, 403);
    assert.equal(mutations, 1);
});
