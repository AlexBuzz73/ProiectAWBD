import { test, expect } from '@playwright/test';

test('real browser: register, login, mutations, transactions and logout/login', async ({ page }) => {
    const errors = [];
    const rejected = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('response', response => {
        if ([401, 403, 500].includes(response.status())) rejected.push(`${response.status()} ${response.url()}`);
    });
    const suffix = Date.now().toString();
    const email = `browser${suffix}@test.com`;
    await page.goto('/register');
    for (const [name, value] of Object.entries({ firstName: 'Browser', lastName: 'Test',
        cnp: suffix, phoneNumber: '0712345678', dateOfBirth: '1999-01-01' })) {
        await page.locator(`[name="${name}"]`).fill(value);
    }
    await page.getByRole('button', { name: 'Continue', exact: true }).click();
    await page.locator('[name="username"]').fill(`browser${suffix}`);
    await page.locator('[name="email"]').fill(email);
    await page.locator('[name="password"]').fill('password123');
    await page.getByRole('button', { name: 'Register', exact: true }).click();
    await expect(page).toHaveURL(/\/login$/);
    const login = async () => {
        await page.locator('[name="email"]').fill(email);
        await page.locator('[name="password"]').fill('password123');
        await page.getByRole('button', { name: 'Login', exact: true }).click();
        await expect(page).toHaveURL(/\/dashboard$/);
    };
    await login();
    // Execute the actual Vite API modules in the browser, with real cookies and CORS.
    const result = await page.evaluate(async () => {
        const { createSingleAccount, getActiveAccountsPaged, closeAccount } = await import('/src/api/accountsApi.js');
        const { createCategory, deleteCategory } = await import('/src/api/categoriesApi.js');
        const { createCard, updateCardStatus } = await import('/src/api/cardsApi.js');
        const { getUserTransactionsPaged, getAccountTransactionsPaged } = await import('/src/api/transactionsApi.js');
        const { transferOwnAccounts, initiatePayment } = await import('/src/api/paymentsApi.js');
        const { updateUserLimits } = await import('/src/api/limitsApi.js');
        const { apiFetch, API_BASE_URL } = await import('/src/api/apiClient.js');
        await updateUserLimits({ maxAmountPerTransactionRon: 5000,
            maxDailyAmountRon: 10000, maxDailyTransactionsCount: 50 });
        const category = await createCategory({ name: 'Browser category' });
        const account = await createSingleAccount({ alias: 'Browser source', currency: 'RON',
            externalIban: 'RO49AAAA1B31007593840000', initialAmount: 1000 });
        const destination = await createSingleAccount({ alias: 'Browser destination', currency: 'RON',
            externalIban: 'RO49AAAA1B31007593840000', initialAmount: 1 });
        const card = await createCard(account.accountId);
        await updateCardStatus(account.accountId, card.cardId, 'BLOCKED');
        const update = await apiFetch(`${API_BASE_URL}/users/me/categories/${category.categoryId}`, {
            method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name: 'Updated browser category' }) });
        if (!update.ok) throw new Error(`Category update ${update.status}`);
        await transferOwnAccounts({ sourceAccountId: account.accountId, destinationAccountId: destination.accountId,
            amount: 1000, categoryId: category.categoryId, password: 'password123', description: 'Browser transfer' });
        const payment = await initiatePayment({ sourceAccountId: destination.accountId,
            destinationIban: 'RO49EXTR0000000000009999', amount: 10, currency: 'RON',
            categoryId: category.categoryId, processingType: 'URGENT', password: 'password123' });
        if (payment.status !== 'EXECUTED') throw new Error('Payment was not executed');
        await closeAccount(account.accountId);
        await deleteCategory(category.categoryId);
        const transactions = await getUserTransactionsPaged(0, 5, 'createdAt', 'desc');
        await getAccountTransactionsPaged(destination.accountId, 0, 5, 'createdAt', 'desc');
        const accounts = await getActiveAccountsPaged(0, 5, 'alias', 'asc');
        return { transactions: transactions.totalElements, accounts: accounts.totalElements };
    });
    expect(result.transactions).toBeGreaterThan(0);
    expect(result.accounts).toBe(1);
    await page.reload();
    await page.getByRole('button', { name: 'Logout', exact: true }).first().click();
    await expect(page).toHaveURL(/\/login$/);
    await login();
    await page.getByRole('link', { name: 'Categorii', exact: true }).click();
    await expect(page).toHaveURL(/\/categories$/);
    expect(errors).toEqual([]);
    expect(rejected).toEqual([]);
});

test('admin: real session, bank limits, unlock, shared account and revoke access', async ({ page }) => {
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('response', response => {
        if (response.status() >= 400) errors.push(`${response.status()} ${response.url()}`);
    });
    await page.goto('/login');
    const emails = await page.evaluate(async () => {
        const { registerUser } = await import('/src/api/authApi.js');
        const stamp = Date.now();
        const emails = [];
        for (let i = 0; i < 2; i++) {
            const name = `shared${stamp}${i}`;
            const email = `${name}@test.com`;
            await registerUser({ individual: { firstName: 'Shared', lastName: 'Test',
                cnp: String(stamp + i), dateOfBirth: '1999-01-01', phoneNumber: '0712345678' },
                user: { username: name, email, password: 'password123' } });
            emails.push(email);
        }
        return emails;
    });
    await page.locator('[name="email"]').fill('admin@test.com');
    await page.locator('[name="password"]').fill('TestAdmin123!');
    await page.getByRole('button', { name: 'Login', exact: true }).click();
    await expect(page).toHaveURL(/\/admin\/dashboard$/);
    await page.evaluate(async ([owner, coowner]) => {
        const { createSharedAccount, revokeAccountAccess, unlockUserByEmail } = await import('/src/api/adminApi.js');
        const { getBankLimits, updateBankLimits } = await import('/src/api/limitApi.js');
        const limits = await getBankLimits();
        await updateBankLimits(limits);
        await unlockUserByEmail(coowner);
        const account = await createSharedAccount({ alias: 'Browser shared', currency: 'RON',
            users: [{ email: owner, role: 'OWNER' }, { email: coowner, role: 'CO_OWNER' }] });
        await revokeAccountAccess(account.accountId, coowner);
    }, emails);
    await page.goto('/admin/bank-limits');
    await expect(page.locator('[name="maxAmountPerTransactionRon"]')).toHaveValue('5000');
    await page.getByRole('button', { name: 'Logout', exact: true }).first().click();
    await expect(page).toHaveURL(/\/login$/);
    expect(errors).toEqual([]);
});

test('security rejects missing/invalid tokens and anonymous access; concurrent bootstrap is shared', async ({ page }) => {
    await page.goto('/login');
    const statuses = await page.evaluate(async () => {
        const { apiFetch, API_BASE_URL } = await import('/src/api/apiClient.js');
        const noAuth = await apiFetch(`${API_BASE_URL}/accounts?userId=1`);
        const missing = await fetch(`${API_BASE_URL}/auth/logout`, { method: 'POST', credentials: 'include' });
        const invalid = await fetch(`${API_BASE_URL}/auth/logout`, { method: 'POST', credentials: 'include', headers: { 'X-XSRF-TOKEN': 'invalid' } });
        const nativeFetch = window.fetch;
        const observed = [];
        window.fetch = (url, options) => { observed.push({ url, credentials: options.credentials }); return nativeFetch(url, options); };
        try {
            // Validation deliberately returns 400 after CSRF succeeds; no database writes.
            const replies = await Promise.all([1, 2, 3].map(() => apiFetch(`${API_BASE_URL}/auth/validate-individual`, {
                method: 'POST', credentials: 'omit', headers: { 'Content-Type': 'application/json' }, body: '{}' })));
            return { noAuth: noAuth.status, missing: missing.status, invalid: invalid.status,
                statuses: replies.map(r => r.status), bootstrap: observed.filter(r => r.url.endsWith('/csrf')).length,
                credentials: observed.every(r => r.credentials === 'include') };
        } finally { window.fetch = nativeFetch; }
    });
    expect(statuses).toEqual({ noAuth: 401, missing: 403, invalid: 403, statuses: [400, 400, 400], bootstrap: 1, credentials: true });
});
