const BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/accounts`;

export async function createSingleAccount(accountData, userId) {
    const response = await fetch(`${BASE_URL}?userId=${userId}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(accountData),
    });

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Account creation failed.");
    }

    return response.json();
}

export async function getActiveAccounts(userId) {
    const response = await fetch(`${BASE_URL}?userId=${userId}`);

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Could not load accounts.");
    }

    return response.json();
}

export async function getAccountDetails(accountId, userId) {
    const response = await fetch(`${BASE_URL}/${accountId}?userId=${userId}`);

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Could not load account details.");
    }

    return response.json();
}

export async function closeAccount(accountId, userId) {
    const response = await fetch(`${BASE_URL}/${accountId}/close?userId=${userId}`, {
        method: "PUT",
    });

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Could not close account.");
    }
}