import {
    loginRequest,
    logoutRequest,
    refreshRequest,
    registerRequest,
} from "../api/auth";
import { ApiError } from "../api/client";

function requireToken(value, fieldName) {
    if (!value || typeof value !== "string") {
        throw new ApiError(`Missing ${fieldName} in response`, 500);
    }

    return value;
}

export async function login({ email, password }) {
    const payload = await loginRequest({ email, password });

    return {
        accessToken: requireToken(payload?.accessToken, "accessToken"),
        refreshToken: requireToken(payload?.refreshToken, "refreshToken"),
        user: payload?.user ?? null,
    };
}

export async function register(payload) {
    return registerRequest(payload);
}

export async function refresh(refreshToken) {
    if (!refreshToken) {
        throw new ApiError("No refresh token available", 401);
    }

    const payload = await refreshRequest(refreshToken);

    return {
        accessToken: requireToken(payload?.accessToken, "accessToken"),
    };
}

export async function logout({ refreshToken, accessToken }) {
    if (!refreshToken) {
        return null;
    }

    return logoutRequest({ refreshToken, accessToken });
}

