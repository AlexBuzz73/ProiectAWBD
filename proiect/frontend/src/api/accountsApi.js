const BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/accounts`;

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

export async function createSingleAccount(accountData, userId) {
    const response = await fetch(`${BASE_URL}?userId=${userId}`, {
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

export async function getActiveAccounts(userId) {
    const response = await fetch(`${BASE_URL}?userId=${userId}`);

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load accounts.");
        throw new Error(message);
    }

    return response.json();
}

export async function getAccountDetails(accountId, userId) {
    const response = await fetch(`${BASE_URL}/${accountId}?userId=${userId}`);

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load account details.");
        throw new Error(message);
    }

    return response.json();
}

export async function closeAccount(accountId, userId) {
    const response = await fetch(`${BASE_URL}/${accountId}/close?userId=${userId}`, {
        method: "PUT",
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not close account.");
        throw new Error(message);
    }
}

export async function getActiveAccountsPaged(userId, page, size, sortBy, direction) {
    const params = new URLSearchParams({userId, page, size, sortBy, direction,});
    const response = await fetch(`${BASE_URL}/paged?${params.toString()}`);

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load paged accounts.");
        throw new Error(message);
    }

    return response.json();
}

export async function getAccountCurrencySummary(userId) {
    const response = await fetch(`${BASE_URL}/summary/currency?userId=${userId}`);

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load account summary.");
        throw new Error(message);
    }

    return response.json();
}