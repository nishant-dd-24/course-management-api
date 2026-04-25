const REFRESH_TOKEN_KEY = "refreshToken";
const USER_KEY = "authUser";

export function readRefreshToken() {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function writeRefreshToken(refreshToken) {
    if (!refreshToken) {
        localStorage.removeItem(REFRESH_TOKEN_KEY);
        return;
    }

    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearRefreshToken() {
    localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function readStoredUser() {
    const raw = localStorage.getItem(USER_KEY);

    if (!raw) {
        return null;
    }

    try {
        return JSON.parse(raw);
    } catch {
        localStorage.removeItem(USER_KEY);
        return null;
    }
}

export function writeStoredUser(user) {
    if (!user) {
        localStorage.removeItem(USER_KEY);
        return;
    }

    localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearStoredUser() {
    localStorage.removeItem(USER_KEY);
}

export function clearStoredSession() {
    clearRefreshToken();
    clearStoredUser();
}

