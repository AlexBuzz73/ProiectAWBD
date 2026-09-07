export const API_BASE_URL =
    (import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');

let csrfRequest = null;

async function getCsrfToken() {
    // Share concurrent bootstrap requests so their Set-Cookie headers cannot race.
    // Refresh for each subsequent mutation, including after login/logout or another tab.
    if (!csrfRequest) {
        csrfRequest = fetch(`${API_BASE_URL}/csrf`, {
            credentials: 'include',
            cache: 'no-store',
        }).then(async (response) => {
            if (!response.ok) throw new Error('Nu s-a putut obtine token-ul CSRF.');
            const data = await response.json();
            if (!data.token) throw new Error('Token CSRF invalid.');
            return data.token;
        }).finally(() => { csrfRequest = null; });
    }
    return csrfRequest;
}

export async function apiFetch(url, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = new Headers(options.headers || {});
    if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
        headers.set('X-XSRF-TOKEN', await getCsrfToken());
    }
    return fetch(url, { ...options, method, headers, credentials: 'include' });
}
