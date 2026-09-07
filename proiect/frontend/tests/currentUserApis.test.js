import { test } from 'node:test';
import assert from 'node:assert/strict';
import { getActiveAccounts, getAccountDetails } from '../src/api/accountsApi.js';
import { getCategories, createCategory } from '../src/api/categoriesApi.js';
import { getCardForAccount } from '../src/api/cardsApi.js';
import { getUserLimits } from '../src/api/limitsApi.js';
import { getUserTransactionsPaged } from '../src/api/transactionsApi.js';

test('current-user API contract omits client identity and retains session/CSRF', async (t) => {
    const calls = [];
    t.mock.method(globalThis, 'fetch', async (url, options) => {
        calls.push({ url: new URL(url), options });
        return new Response(JSON.stringify(url.endsWith('/csrf') ? { token: 'token' } : []));
    });
    await getActiveAccounts();
    await getAccountDetails(7);
    await getCategories();
    await createCategory({ name: 'Private' });
    await getCardForAccount(7);
    await getUserLimits();
    await getUserTransactionsPaged(0, 5, 'createdAt', 'desc');
    assert.ok(calls.every(c => !c.url.searchParams.has('userId') && c.options.credentials === 'include'));
    assert.ok(calls.some(c => c.url.pathname === '/api/users/me/categories'));
    assert.ok(calls.some(c => c.url.pathname === '/api/users/me/accounts/7/card'));
    assert.ok(calls.some(c => c.url.pathname === '/api/user/me/limits'));
    assert.equal(calls.find(c => c.options.method === 'POST').options.headers.get('X-XSRF-TOKEN'), 'token');
});
