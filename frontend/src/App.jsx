import { useCallback, useEffect, useMemo, useSyncExternalStore } from "react";
import { useAuth } from "./auth/AuthContext";
import CoursesPage from "./pages/CoursesPage";
import Dashboard from "./pages/Dashboard";
import EnrollmentsPage from "./pages/EnrollmentsPage";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Users from "./pages/Users";
import Button from "./components/ui/Button";
import Card from "./components/ui/Card";

const DASHBOARD_PATH = "/dashboard";
const LOGIN_PATH = "/login";
const REGISTER_PATH = "/register";
const PROTECTED_PREFIXES = ["/dashboard", "/courses", "/enrollments", "/users"];

function subscribeToLocation(callback) {
    window.addEventListener("popstate", callback);
    return () => window.removeEventListener("popstate", callback);
}

function getLocationSnapshot() {
    return `${window.location.pathname}${window.location.search}`;
}

function isProtectedPath(pathname) {
    return PROTECTED_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`));
}

function normalizeRedirect(rawRedirect) {
    if (!rawRedirect || !rawRedirect.startsWith("/")) {
        return DASHBOARD_PATH;
    }

    if (
        rawRedirect === LOGIN_PATH
        || rawRedirect.startsWith(`${LOGIN_PATH}?`)
        || rawRedirect === REGISTER_PATH
        || rawRedirect.startsWith(`${REGISTER_PATH}?`)
    ) {
        return DASHBOARD_PATH;
    }

    return rawRedirect;
}

function buildLoginNotice(search) {
    const params = new URLSearchParams(search);

    if (params.get("registered") !== "1") {
        return "";
    }

    const email = params.get("email");

    if (email) {
        return `Account created for ${email}. Please sign in.`;
    }

    return "Account created successfully. Please sign in.";
}

function FullScreenMessage({ text }) {
    return (
        <div className="flex min-h-screen items-center justify-center bg-zinc-950 px-4 text-zinc-100">
            <Card className="px-6 py-4 text-sm text-zinc-300">{text}</Card>
        </div>
    );
}

function FeaturePlaceholder({ title, onBack }) {
    return (
        <div className="min-h-screen bg-zinc-950 px-4 py-8 text-zinc-100">
            <Card className="mx-auto max-w-2xl space-y-4 text-center">
                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-zinc-800 text-2xl">
                    ⚠️
                </div>
                <div className="space-y-2">
                    <h1 className="text-2xl font-semibold tracking-tight text-zinc-50">{title}</h1>
                    <p className="text-sm leading-6 text-zinc-300">
                        This page is the next step after dashboard navigation and can be implemented incrementally.
                    </p>
                </div>
                <div className="flex justify-center">
                    <Button type="button" variant="primary" onClick={onBack}>
                        Back to Dashboard
                    </Button>
                </div>
            </Card>
        </div>
    );
}

export default function App() {
    const { status, isAuthenticated, currentUser, logout } = useAuth();
    const locationKey = useSyncExternalStore(subscribeToLocation, getLocationSnapshot, getLocationSnapshot);

    const location = useMemo(() => {
        const [pathname, rawSearch = ""] = locationKey.split("?");

        return {
            pathname,
            search: rawSearch ? `?${rawSearch}` : "",
        };
    }, [locationKey]);

    const navigate = useCallback((nextPath, options = {}) => {
        const method = options.replace ? "replaceState" : "pushState";

        if (`${window.location.pathname}${window.location.search}` === nextPath) {
            return;
        }

        window.history[method]({}, "", nextPath);
        window.dispatchEvent(new PopStateEvent("popstate"));
    }, []);

    useEffect(() => {
        if (status === "loading") {
            return;
        }

        if (!isAuthenticated && isProtectedPath(location.pathname)) {
            const redirect = encodeURIComponent(`${location.pathname}${location.search}`);
            navigate(`${LOGIN_PATH}?redirect=${redirect}`, { replace: true });
            return;
        }

        if (
            isAuthenticated
            && (location.pathname === "/" || location.pathname === LOGIN_PATH || location.pathname === REGISTER_PATH)
        ) {
            const params = new URLSearchParams(location.search);
            const redirect = normalizeRedirect(params.get("redirect"));
            navigate(redirect, { replace: true });
        }
    }, [status, isAuthenticated, location.pathname, location.search, navigate]);

    if (status === "loading") {
        return <FullScreenMessage text="Restoring session..." />;
    }

    if (!isAuthenticated) {
        const params = new URLSearchParams(location.search);
        const redirectTo = normalizeRedirect(params.get("redirect"));
        const loginNotice = buildLoginNotice(location.search);

        if (location.pathname === REGISTER_PATH) {
            return <Register onNavigate={navigate} />;
        }

        return <Login onSuccessRedirect={redirectTo} onNavigate={navigate} notice={loginNotice} />;
    }

    if (location.pathname === DASHBOARD_PATH) {
        return <Dashboard user={currentUser} onLogout={logout} onNavigate={navigate} />;
    }

    if (location.pathname === "/users") {
        if (currentUser?.role !== "ADMIN") {
            return <FeaturePlaceholder title="403 - Admin access required" onBack={() => navigate(DASHBOARD_PATH)} />;
        }

        return <Users onNavigate={navigate} />;
    }

    if (location.pathname === "/courses") {
        return <CoursesPage onNavigate={navigate} />;
    }

    if (location.pathname === "/enrollments") {
        return <EnrollmentsPage onNavigate={navigate} search={location.search} />;
    }

    return <FeaturePlaceholder title="Page not found" onBack={() => navigate(DASHBOARD_PATH, { replace: true })} />;
}
