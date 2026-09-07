const BASE_URL = `${API_BASE_URL}/user`;
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

export async function getUserLimits() {
    const response = await apiFetch(`${BASE_URL}/me/limits`, { credentials: 'include' });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load user limits.");
        throw new Error(message);
    }

    return response.json();
}

export async function updateUserLimits(limitsData) {
    const response = await apiFetch(`${BASE_URL}/me/limits`, {
        method: "PUT",
        credentials: 'include',
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(limitsData),
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not update user limits.");
        throw new Error(message);
    }

    return response.json();
}