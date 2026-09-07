const BASE_URL = `${API_BASE_URL}/transactions`;
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

export async function getUserTransactionsPaged(page, size, sortBy, direction) {
    const params = new URLSearchParams({page, size, sortBy, direction,});
    const response = await apiFetch(`${BASE_URL}/user?${params.toString()}`, { credentials: 'include' });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load transactions.");
        throw new Error(message);
    }

    return response.json();
}

export async function getAccountTransactionsPaged(accountId, page, size, sortBy, direction) {
    const params = new URLSearchParams({page, size, sortBy, direction,});
    const response = await apiFetch(`${BASE_URL}/account/${accountId}?${params.toString()}`, { credentials: 'include' });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load account transactions.");
        throw new Error(message);
    }

    return response.json();
}