const BASE_URL = `${API_BASE_URL}/users`;
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

export async function getCardForAccount(accountId) {
    const response = await apiFetch(`${BASE_URL}/me/accounts/${accountId}/card`, { credentials: 'include' });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load card.");
        throw new Error(message);
    }

    const text = await response.text();

    if (!text) {
        return null;
    }

    return JSON.parse(text);
}

export async function createCard(accountId) {
    const response = await apiFetch(`${BASE_URL}/me/accounts/${accountId}/card`, {
        method: "POST",
        credentials: 'include',
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not create card.");
        throw new Error(message);
    }

    return response.json();
}

export async function updateCardStatus(accountId, cardId, status) {
    const response = await apiFetch(`${BASE_URL}/me/accounts/${accountId}/card/${cardId}/status/${status}`, {
        method: "PATCH",
        credentials: 'include',
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not update card status.");
        throw new Error(message);
    }
}
