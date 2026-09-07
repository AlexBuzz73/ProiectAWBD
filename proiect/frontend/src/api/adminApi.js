const BASE_URL = `${API_BASE_URL}/admin`;
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

export async function unlockUserByEmail(email) {
    const response = await apiFetch(`${BASE_URL}/unlock-user?email=${encodeURIComponent(email)}`, {
        method: "POST",
        credentials: 'include',
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not unlock user.");
        throw new Error(message);
    }

    return response.text();
}

export async function createSharedAccount(sharedAccountData) {
    const response = await apiFetch(`${BASE_URL}/create-shared-account`, {
        method: "POST",
        credentials: 'include',
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(sharedAccountData),
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not create shared account.");
        throw new Error(message);
    }

    return response.json();
}

export async function revokeAccountAccess(accountId, email) {
    const response = await apiFetch(`${BASE_URL}/accounts/${accountId}/access?email=${encodeURIComponent(email)}`, {
        method: "DELETE",
        credentials: 'include',
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not revoke account access.");
        throw new Error(message);
    }
}
