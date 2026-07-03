import Badge from "./Badge";
import Button from "./Button";

function getNavItems(role) {
    if (role === "ADMIN") {
        return [
            { label: "Dashboard", path: "/dashboard" },
            { label: "Courses", path: "/courses" },
            { label: "Users", path: "/users" },
        ];
    }

    return [
        { label: "Dashboard", path: "/dashboard" },
        { label: "Courses", path: "/courses" },
        { label: "Enrollments", path: "/enrollments" },
    ];
}

export default function Navigation({ user, onNavigate, onLogout }) {
    const items = getNavItems(user?.role);
    const pathname = window.location.pathname;

    return (
        <header className="border-b border-slate-800/80 bg-slate-950/90 backdrop-blur supports-[backdrop-filter]:bg-slate-950/75">
            <div className="mx-auto flex w-full max-w-6xl flex-col gap-4 px-4 py-4 lg:flex-row lg:items-center lg:justify-between">
                <button
                    type="button"
                    onClick={() => onNavigate?.("/dashboard")}
                    className="flex items-center gap-3 rounded-2xl px-2 py-1 text-left transition hover:bg-slate-900/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/30"
                >
                    <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-500 text-sm font-semibold text-white shadow-lg shadow-blue-500/20">
                        CM
                    </div>
                    <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">Course Management</p>
                        <div className="mt-1 flex flex-wrap items-center gap-2">
                            <span className="text-sm font-medium text-slate-100">{user?.name ?? "User"}</span>
                            <Badge variant={user?.role?.toLowerCase() ?? "neutral"}>{user?.role ?? "UNKNOWN"}</Badge>
                        </div>
                    </div>
                </button>

                <nav className="flex flex-wrap items-center gap-2">
                    {items.map((item) => {
                        const isActive = pathname === item.path || pathname.startsWith(`${item.path}/`);

                        return (
                            <Button
                                key={item.path}
                                variant={isActive ? "primary" : "outline"}
                                size="sm"
                                onClick={() => onNavigate?.(item.path)}
                                className="min-w-[94px]"
                            >
                                {item.label}
                            </Button>
                        );
                    })}

                    <Button variant="outline" size="sm" onClick={onLogout} className="border-slate-700/90 text-slate-300">
                        Logout
                    </Button>
                </nav>
            </div>
        </header>
    );
}

