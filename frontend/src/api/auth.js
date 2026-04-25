import { apiFetch } from "./client";

export function registerRequest(payload) {
    return apiFetch("/users/register", {
        method: "POST",
        body: JSON.stringify(payload),
        skipAuth: true,
        skipAuthRefresh: true,
    });
}

export function loginRequest({ email, password }) {
    return apiFetch("/users/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
        skipAuth: true,
        skipAuthRefresh: true,
    });
}

export function refreshRequest(refreshToken) {
    return apiFetch("/users/refresh", {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
        skipAuth: true,
        skipAuthRefresh: true,
    });
}

export function logoutRequest({ refreshToken, accessToken }) {
    return apiFetch("/users/logout", {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
        headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined,
        skipAuth: true,
        skipAuthRefresh: true,
    });
}