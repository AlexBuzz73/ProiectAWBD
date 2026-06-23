const BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/payments`;

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

export async function initiatePayment(userId, paymentData) {
    const response = await fetch(`${BASE_URL}/initiate?userId=${userId}`, {
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

export async function transferOwnAccounts(userId, transferData) {
    const response = await fetch(`${BASE_URL}/transfer-own?userId=${userId}`, {
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

export async function exchangeCurrency(userId, exchangeData) {
    const response = await fetch(`${BASE_URL}/exchange?userId=${userId}`, {
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
