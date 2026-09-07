const BASE_URL = `${API_BASE_URL}/accounts`;
import { apiFetch, API_BASE_URL } from './apiClient.js';
async function getErrorMessage(response, fallbackMessage) {
    const contentType = response.headers.get("content-type");

    if (contentType && contentType.includes("application/json")) {
        const errorBody = await response.json();
        const messages = Object.values(errorBody);

        return messages.length > 0 ? messages[0] : fallbackMessage;
    }

    const message = await response.text();
    return message || fallbackMessage;
}

export async function createSingleAccount(accountData) {
    const response = await apiFetch(`${BASE_URL}`, {
        credentials: 'include',
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(accountData),
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Account creation failed.");
        throw new Error(message);
    }

    return response.json();
}

export async function getActiveAccounts() {
    const response = await apiFetch(`${BASE_URL}`, {
        credentials: 'include' });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load accounts.");
        throw new Error(message);
    }

    return response.json();
}

export async function getAccountDetails(accountId) {
    const response = await apiFetch(`${BASE_URL}/${accountId}`, { credentials: 'include' });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load account details.");
        throw new Error(message);
    }

    return response.json();
}

export async function closeAccount(accountId) {
    const response = await apiFetch(`${BASE_URL}/${accountId}/close`, {
        credentials: 'include',
        method: "PUT",
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not close account.");
        throw new Error(message);
    }
}

export async function getActiveAccountsPaged(page, size, sortBy, direction) {
    const params = new URLSearchParams({page, size, sortBy, direction,});
    const response = await apiFetch(`${BASE_URL}/paged?${params.toString()}`, { credentials: 'include' });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load paged accounts.");
        throw new Error(message);
    }

    return response.json();
}

export async function getAccountCurrencySummary() {
    const response = await apiFetch(`${BASE_URL}/summary/currency`, { credentials: 'include' });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load account summary.");
        throw new Error(message);
    }

    return response.json();
}