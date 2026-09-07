import { test, expect } from '@playwright/test';

test('Remember Me preserves authentication across session cookie loss and 404 is handled gracefully', async ({ context, page }) => {
    const suffix = Date.now().toString();
    const email = `remember${suffix}@test.com`;

    // 1. Register a new user
    await page.goto('/register');
    for (const [name, value] of Object.entries({
        firstName: 'Remember', lastName: 'Tester',
        cnp: suffix, phoneNumber: '0712345678', dateOfBirth: '1995-05-15'
    })) {
        await page.locator(`[name="${name}"]`).fill(value);
    }
    await page.getByRole('button', { name: 'Continue', exact: true }).click();
    await page.locator('[name="username"]').fill(`remember${suffix}`);
    await page.locator('[name="email"]').fill(email);
    await page.locator('[name="password"]').fill('Password123!');
    await page.getByRole('button', { name: 'Register', exact: true }).click();
    await expect(page).toHaveURL(/\/login$/);

    // 2. Login with Remember Me checked
    await page.locator('[name="email"]').fill(email);
    await page.locator('[name="password"]').fill('Password123!');
    await page.locator('[name="rememberMe"]').check();
    await page.getByRole('button', { name: 'Login', exact: true }).click();
    await expect(page).toHaveURL(/\/dashboard$/);

    // 3. Verify remember-me cookie is present
    let cookies = await context.cookies();
    const rememberCookie = cookies.find(c => c.name === 'remember-me');
    expect(rememberCookie).toBeDefined();
    expect(rememberCookie.value.length).toBeGreaterThan(10);

    // 4. Simulate browser restart / session expiration: remove JSESSIONID cookie but keep remember-me
    await context.clearCookies({ name: 'JSESSIONID' });
    cookies = await context.cookies();
    expect(cookies.find(c => c.name === 'JSESSIONID')).toBeUndefined();
    expect(cookies.find(c => c.name === 'remember-me')).toBeDefined();

    // 5. Access authenticated endpoint via API: remember-me autoLogin should authenticate request
    const accountsResult = await page.evaluate(async () => {
        const { apiFetch, API_BASE_URL } = await import('/src/api/apiClient.js');
        const res = await apiFetch(`${API_BASE_URL}/accounts`);
        return { status: res.status, data: await res.json() };
    });
    expect(accountsResult.status).toBe(200);

    // 6. Test 404 on frontend route
    await page.goto('/does-not-exist-route-12345');
    await expect(page.getByText('404 - Pagina nu a fost găsită')).toBeVisible();

    // 7. Test 404 on backend unmapped URL
    const apiNotFoundResult = await page.evaluate(async () => {
        const { apiFetch, API_BASE_URL } = await import('/src/api/apiClient.js');
        const res = await apiFetch(`${API_BASE_URL}/does-not-exist`);
        return { status: res.status, body: await res.json() };
    });
    expect(apiNotFoundResult.status).toBe(404);
    expect(apiNotFoundResult.body.error).toBe('Resursa nu a fost gasita');
});
