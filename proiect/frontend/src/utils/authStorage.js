export function saveLoggedUser(user) {
    localStorage.setItem("loggedUser", JSON.stringify(user));
}

export function getLoggedUser() {
    const storedUser = localStorage.getItem("loggedUser");

    if (!storedUser) {
        return null;
    }

    return JSON.parse(storedUser);
}

export function removeLoggedUser() {
    localStorage.removeItem("loggedUser");
}