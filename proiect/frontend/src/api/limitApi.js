const BASE_URL = "http://localhost:8080/api";

export async function getBankLimits() {
    const response = await fetch('${BASE_URL}/admin/bank-limits');

    if (!response.ok) {
        throw new Error("The bank-limits could not be found.");
    }

    return response.json();
}

export async function updateBankLimits(data) {
    const response = await fetch('${BASE_URL}/admin/bank-limits', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    });

    if (!response.ok) {
        throw new Error("The bank-limits could not be updated.");
    }

    return response.json();
}

export async function deleteBankLimits(id) {
    const response = await fetch(`${BASE_URL}/admin/bank-limits/${id}`, {
        method: 'DELETE',
    });

    if (!response.ok) {
        throw new Error("The bank-limits could not be deleted.");
    }
}

export async function getUserLimits(userId) {
    const response = await fetch('${BASE_URL}/user/${userId}/limits');

    if (!response.ok) {
        throw new Error("The user-limits could not be found.");
    }

    return response.json();
}

export async function updateUserLimits(data) {
    const response = await fetch('${BASE_URL}/user/${userId}/limits', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    });

    if (!response.ok) {
        throw new Error("The user-limits could not be updated.");
    }

    return response.json();
}

export async function deleteUserLimits(id) {
    const response = await fetch(`${BASE_URL}/user/${id}/limits`, {
        method: 'DELETE',
    });

    if (!response.ok) {
        throw new Error("The user-limits could not be deleted.");
    }
}