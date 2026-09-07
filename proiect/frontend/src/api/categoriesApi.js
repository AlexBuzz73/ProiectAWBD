const BASE_URL = `${API_BASE_URL}/users`;
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

export async function getCategories() {
    const response = await apiFetch(`${BASE_URL}/me/categories`, { credentials: 'include' });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load categories.");
        throw new Error(message);
    }

    return response.json();
}

export async function getCategoriesPaged(page, size, sortBy, direction) {
    const params = new URLSearchParams({ page, size, sortBy, direction });
    const response = await apiFetch(`${BASE_URL}/me/categories/paged?${params.toString()}`, { credentials: 'include' });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not load categories.");
        throw new Error(message);
    }

    return response.json();
}

export async function createCategory(categoryData) {
    const response = await apiFetch(`${BASE_URL}/me/categories`, {
        method: "POST",
        credentials: 'include',
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

export async function deleteCategory(categoryId) {
    const response = await apiFetch(`${BASE_URL}/me/categories/${categoryId}`, {
        method: "DELETE",
        credentials: 'include',
    });

    if (!response.ok) {
        const message = await getErrorMessage(response, "Could not delete category.");
        throw new Error(message);
    }
}