const BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/auth`

export async function validateIndividual(individualData) {
    const response = await fetch(`${BASE_URL}/validate-individual`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(individualData),
    });

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Individual validation failed.");
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
        const message = await response.text();
        throw new Error(message || "Registration failed.");
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
        const message = await response.text();
        throw new Error(message || "Login failed.");
    }

    return response.json();
}