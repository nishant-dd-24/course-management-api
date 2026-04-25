const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

let accessTokenResolver = () => null;
let refreshSessionHandler = null;
let authFailureHandler = null;
let refreshPromise = null;

export class ApiError extends Error {
    constructor(message, status, data = null) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.data = data;
    }
}

export function configureApiClient({ getAccessToken, refreshSession, onAuthFailure }) {
    if (typeof getAccessToken === "function") {
        accessTokenResolver = getAccessToken;
    }

    if (typeof refreshSession === "function") {
        refreshSessionHandler = refreshSession;
    }

    if (typeof onAuthFailure === "function") {
        authFailureHandler = onAuthFailure;
    }
}

function buildUrl(path) {
    return `${API_BASE}${path}`;
}

function buildHeaders(headers = {}, accessToken) {
    return {
        "Content-Type": "application/json",
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        ...headers,
    };
}

async function parseBody(response) {
    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("content-type") ?? "";

    if (contentType.includes("application/json")) {
        return response.json();
    }

    const text = await response.text();
    return text || null;
}

async function executeRequest(path, options, accessTokenOverride = null) {
    const {
        skipAuth = false,
        skipAuthRefresh = false,
        headers,
        ...fetchOptions
    } = options;

    const accessToken = skipAuth ? null : accessTokenOverride ?? accessTokenResolver();

    const response = await fetch(buildUrl(path), {
        ...fetchOptions,
        headers: buildHeaders(headers, accessToken),
    });

    const data = await parseBody(response);

    return {
        response,
        data,
        meta: { skipAuthRefresh },
    };
}

function asApiError(response, data) {
    const message =
        (data && typeof data === "object" && data.message) ||
        (typeof data === "string" && data) ||
        "Request failed";

    return new ApiError(message, response.status, data);
}

async function refreshAccessToken() {
    if (!refreshSessionHandler) {
        throw new ApiError("Refresh handler is not configured", 500);
    }

    if (!refreshPromise) {
        refreshPromise = (async () => {
            try {
                return await refreshSessionHandler();
            } catch (error) {
                authFailureHandler?.(error);
                throw error;
            } finally {
                refreshPromise = null;
            }
        })();
    }

    return refreshPromise;
}

export async function apiFetch(path, options = {}) {
    const first = await executeRequest(path, options);

    if (first.response.ok) {
        return first.data;
    }

    if (first.response.status === 401 && !first.meta.skipAuthRefresh) {
        const refreshedAccessToken = await refreshAccessToken();

        const retried = await executeRequest(path, {
            ...options,
            skipAuthRefresh: true,
        }, refreshedAccessToken);

        if (retried.response.ok) {
            return retried.data;
        }

        throw asApiError(retried.response, retried.data);
    }

    throw asApiError(first.response, first.data);
}
