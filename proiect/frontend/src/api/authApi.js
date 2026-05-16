const BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/auth`

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

export async function validateIndividual(individualData) {
    const response = await fetch(`${BASE_URL}/validate-individual`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(individualData),
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Individual validation failed.");
        throw new Error(message);
    }
}

export async function registerUser(registrationData) {
    const response = await fetch(`${BASE_URL}/register`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(registrationData),
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Registration failed.");
        throw new Error(message);
    }
}

export async function loginUser(loginData) {
    const response = await fetch(`${BASE_URL}/login`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(loginData),
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Login failed.");
        throw new Error(message);
    }

    return response.json();
}