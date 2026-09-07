import { test } from 'node:test';
import assert from 'node:assert/strict';
import { loginUser } from '../src/api/authApi.js';

test('loginUser forwards rememberMe flag and credentials', async (t) => {
    let captured = null;
    t.mock.method(globalThis, 'fetch', async (url, options) => {
        if (url.endsWith('/csrf')) return new Response(JSON.stringify({ token: 'csrf-token' }));
        captured = { url, options, body: JSON.parse(options.body) };
        return new Response(JSON.stringify({ email: 'test@example.com', role: 'USER' }));
    });

    const user = await loginUser({ email: 'test@example.com', password: 'Password123!', rememberMe: true });
    assert.equal(user.email, 'test@example.com');
    assert.ok(captured);
    assert.equal(captured.body.email, 'test@example.com');
    assert.equal(captured.body.rememberMe, true);
    assert.equal(captured.options.credentials, 'include');
});

test('loginUser throws server error message on 400/401/403/404/500 JSON', async (t) => {
    t.mock.method(globalThis, 'fetch', async (url) => {
        if (url.endsWith('/csrf')) return new Response(JSON.stringify({ token: 'csrf-token' }));
        return new Response(JSON.stringify({ error: 'Resursa nu a fost gasita' }), {
            status: 404,
            headers: { 'Content-Type': 'application/json' },
        });
    });

    await assert.rejects(
        loginUser({ email: 'bad@test.com', password: 'wrong' }),
        /Resursa nu a fost gasita/
    );
});
