import { test, expect } from '@playwright/test';

test('two real sessions: forged user and resource IDs are forbidden, own data remains accessible', async ({ browser }) => {
    const contexts = [await browser.newContext(), await browser.newContext()];
    try {
        const pages = await Promise.all(contexts.map(context => context.newPage()));
        const resources = [];
        for (let i = 0; i < pages.length; i++) {
            await pages[i].goto('http://localhost:5173/login');
            resources.push(await pages[i].evaluate(async (suffix) => {
                const { registerUser, loginUser } = await import('/src/api/authApi.js');
                const { createSingleAccount } = await import('/src/api/accountsApi.js');
                const { createCategory } = await import('/src/api/categoriesApi.js');
                const { createCard } = await import('/src/api/cardsApi.js');
                const { updateUserLimits } = await import('/src/api/limitsApi.js');
                const email = `ownership${suffix}@test.com`;
                await registerUser({ individual: { firstName: 'Ownership', lastName: 'Test', cnp: suffix,
                    phoneNumber: '0712345678', dateOfBirth: '1999-01-01' },
                    user: { username: `ownership${suffix}`, email, password: 'password123' } });
                const user = await loginUser({ email, password: 'password123' });
                const account = await createSingleAccount({ alias: email, currency: 'RON', initialAmount: 100,
                    externalIban: 'RO49AAAA1B31007593840000' });
                const category = await createCategory({ name: email });
                const card = await createCard(account.accountId);
                await updateUserLimits({ maxAmountPerTransactionRon: 1000, maxDailyAmountRon: 2000, maxDailyTransactionsCount: 5 });
                return { user, account, category, card };
            }, String(Date.now() + i)));
        }
        const result = await pages[0].evaluate(async ([a, b]) => {
            const { apiFetch, API_BASE_URL } = await import('/src/api/apiClient.js');
            const { getActiveAccounts } = await import('/src/api/accountsApi.js');
            // Altering localStorage cannot alter the backend's identity.
            localStorage.setItem('loggedUser', JSON.stringify(b.user));
            const own = await getActiveAccounts();
            const statuses = [];
            for (const [method, path, body] of [
                ['GET', `/accounts?userId=${b.user.userId}`],
                ['GET', `/accounts/${b.account.accountId}`],
                ['GET', `/transactions/account/${b.account.accountId}`],
                ['PUT', `/accounts/${b.account.accountId}/close`],
                ['GET', `/users/${b.user.userId}/categories`],
                ['PUT', `/users/me/categories/${b.category.categoryId}`, { name: 'stolen' }],
                ['DELETE', `/users/me/categories/${b.category.categoryId}`],
                ['PATCH', `/users/me/accounts/${b.account.accountId}/card/${b.card.cardId}/status/BLOCKED`],
                ['DELETE', `/users/me/accounts/${a.account.accountId}/card/${b.card.cardId}/delete`],
                ['GET', `/user/${b.user.userId}/limits`],
                ['PUT', `/user/${b.user.userId}/limits`, { maxAmountPerTransactionRon: 1, maxDailyAmountRon: 2, maxDailyTransactionsCount: 1 }],
                ['POST', '/payments/transfer-own', { sourceAccountId: b.account.accountId, destinationAccountId: a.account.accountId,
                    amount: 10, categoryId: a.category.categoryId, password: 'password123' }],
                ['GET', '/admin/bank-limits'],
            ]) {
                const response = await apiFetch(API_BASE_URL + path, { method,
                    headers: { 'Content-Type': 'application/json' }, body: body ? JSON.stringify(body) : undefined });
                statuses.push(response.status);
            }
            return { own: own.map(account => account.accountId), statuses };
        }, resources);
        expect(result.own).toEqual([resources[0].account.accountId]);
        expect(result.statuses).toEqual(Array(13).fill(403));
        const unchanged = await pages[1].evaluate(async ({ account }) => {
            const { getAccountDetails } = await import('/src/api/accountsApi.js');
            const { getCardForAccount } = await import('/src/api/cardsApi.js');
            const { getCategories } = await import('/src/api/categoriesApi.js');
            return { account: await getAccountDetails(account.accountId), card: await getCardForAccount(account.accountId), categories: await getCategories() };
        }, resources[1]);
        expect(unchanged.account.balance).toBe(100);
        expect(unchanged.card.status).toBe('ACTIVE');
        expect(unchanged.categories.some(c => c.categoryId === resources[1].category.categoryId)).toBe(true);
    } finally {
        await Promise.all(contexts.map(context => context.close()));
    }
});
