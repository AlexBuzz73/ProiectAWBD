const BASE_URL = `${API_BASE_URL}/payments`;
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

export async function initiatePayment(paymentData) {
    const response = await apiFetch(`${BASE_URL}/initiate`, {
        method: "POST",
        credentials: 'include',
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(paymentData),
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Payment could not be initiated.");
        throw new Error(message);
    }

    return response.json();
}

export async function transferOwnAccounts(transferData) {
    const response = await apiFetch(`${BASE_URL}/transfer-own`, {
        method: "POST",
        credentials: 'include',
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(transferData),
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Transfer could not be completed.");
        throw new Error(message);
    }

    return response.json();
}

export async function exchangeCurrency(exchangeData) {
    const response = await apiFetch(`${BASE_URL}/exchange`, {
        method: "POST",
        credentials: 'include',
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(exchangeData),
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Currency exchange could not be completed.");
        throw new Error(message);
    }

    return response.json();
}
