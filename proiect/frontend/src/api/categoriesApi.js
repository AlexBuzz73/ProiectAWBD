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

export async function getCategoriesPaged(userId, page, size, sortBy, direction) {
    const params = new URLSearchParams({ page, size, sortBy, direction });
    const response = await fetch(`${BASE_URL}/${userId}/categories/paged?${params.toString()}`);

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load categories.");
        throw new Error(message);
    }

    return response.json();
}

export async function createCategory(userId, categoryData) {
    const response = await fetch(`${BASE_URL}/${userId}/categories`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(categoryData),
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not create category.");
        throw new Error(message);
    }

    return response.json();
}

export async function deleteCategory(userId, categoryId) {
    const response = await fetch(`${BASE_URL}/${userId}/categories/${categoryId}`, {
        method: "DELETE",
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not delete category.");
        throw new Error(message);
    }
}