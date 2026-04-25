/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { configureApiClient } from "../api/client";
import * as authService from "../services/authService";
import {
    clearStoredSession,
    readRefreshToken,
    readStoredUser,
    writeRefreshToken,
    writeStoredUser,
} from "./tokenStore";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const initialRefreshToken = readRefreshToken();

    const [authState, setAuthState] = useState(() => ({
        status: initialRefreshToken ? "loading" : "anonymous",
        accessToken: null,
        refreshToken: initialRefreshToken,
        user: readStoredUser(),
    }));

    const sessionVersionRef = useRef(0);

    const clearSession = useCallback(() => {
        sessionVersionRef.current += 1;
        clearStoredSession();
        setAuthState({
            status: "anonymous",
            accessToken: null,
            refreshToken: null,
            user: null,
        });
    }, []);

    const applyLoginSession = useCallback(({ accessToken, refreshToken, user }) => {
        sessionVersionRef.current += 1;
        writeRefreshToken(refreshToken);
        writeStoredUser(user);

        setAuthState({
            status: "authenticated",
            accessToken,
            refreshToken,
            user,
        });
    }, []);

    const refreshSession = useCallback(async () => {
        const versionAtStart = sessionVersionRef.current;
        const storedRefreshToken = readRefreshToken();

        if (!storedRefreshToken) {
            throw new Error("No refresh token available");
        }

        const refreshed = await authService.refresh(storedRefreshToken);

        if (sessionVersionRef.current !== versionAtStart) {
            throw new Error("Session changed during refresh");
        }

        setAuthState((prev) => ({
            ...prev,
            status: "authenticated",
            accessToken: refreshed.accessToken,
            refreshToken: storedRefreshToken,
        }));

        return refreshed.accessToken;
    }, []);

    const login = useCallback(async ({ email, password }) => {
        const session = await authService.login({ email, password });
        applyLoginSession(session);
    }, [applyLoginSession]);

    const logout = useCallback(async () => {
        const snapshot = {
            refreshToken: readRefreshToken(),
            accessToken: authState.accessToken,
        };

        clearSession();

        try {
            await authService.logout(snapshot);
        } catch {
            // Local session is already cleared; remote logout failure is non-blocking.
        }
    }, [authState.accessToken, clearSession]);

    useEffect(() => {
        configureApiClient({
            getAccessToken: () => authState.accessToken,
            refreshSession,
            onAuthFailure: clearSession,
        });
    }, [authState.accessToken, refreshSession, clearSession]);

    useEffect(() => {
        let isActive = true;
        const storedRefreshToken = readRefreshToken();

        if (!storedRefreshToken) {
            return undefined;
        }

        // eslint-disable-next-line react-hooks/set-state-in-effect
        refreshSession()
            .catch(() => {
                if (isActive) {
                    clearSession();
                }
            });

        return () => {
            isActive = false;
        };
    }, [refreshSession, clearSession]);

    useEffect(() => {
        function handleStorage(event) {
            if (event.key !== "refreshToken" && event.key !== "authUser") {
                return;
            }

            const nextRefreshToken = readRefreshToken();
            const nextUser = readStoredUser();

            if (!nextRefreshToken) {
                clearSession();
                return;
            }

            setAuthState((prev) => ({
                ...prev,
                refreshToken: nextRefreshToken,
                user: nextUser,
            }));
        }

        window.addEventListener("storage", handleStorage);
        return () => window.removeEventListener("storage", handleStorage);
    }, [clearSession]);

    const value = useMemo(
        () => ({
            status: authState.status,
            isAuthenticated: authState.status === "authenticated",
            accessToken: authState.accessToken,
            refreshToken: authState.refreshToken,
            currentUser: authState.user,
            login,
            logout,
            clearSession,
        }),
        [authState, login, logout, clearSession]
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const context = useContext(AuthContext);

    if (!context) {
        throw new Error("useAuth must be used within AuthProvider");
    }

    return context;
}
