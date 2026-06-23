const BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/users`;

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

export async function getCardForAccount(userId, accountId) {
    const response = await fetch(`${BASE_URL}/${userId}/accounts/${accountId}/card`, { credentials: 'include' });

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

export async function createCard(userId, accountId) {
    const response = await fetch(`${BASE_URL}/${userId}/accounts/${accountId}/card`, {
        method: "POST",
        credentials: 'include',
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not create card.");
        throw new Error(message);
    }

    return response.json();
}

export async function updateCardStatus(userId, accountId, cardId, status) {
    const response = await fetch(`${BASE_URL}/${userId}/accounts/${accountId}/card/${cardId}/status/${status}`, {
        method: "PATCH",
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not update card status.");
        throw new Error(message);
    }
}
