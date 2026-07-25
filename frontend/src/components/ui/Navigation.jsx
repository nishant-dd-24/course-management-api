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
        <header className="border-b border-zinc-800/80 bg-zinc-950/90 backdrop-blur supports-[backdrop-filter]:bg-zinc-950/75">
            <div className="mx-auto flex w-full max-w-6xl flex-col gap-4 px-4 py-4 lg:flex-row lg:items-center lg:justify-between">
                <button
                    type="button"
                    onClick={() => onNavigate?.("/dashboard")}
                    className="flex items-center gap-3 rounded-2xl px-2 py-1 text-left transition hover:bg-zinc-900/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/30"
                >
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center">
                        <img src="/logo.png" alt="Course Management Logo" className="h-full w-full object-contain" />
                    </div>
                    <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-zinc-500">Course Management</p>
                        <div className="mt-1 flex flex-wrap items-center gap-2">
                            <span className="text-sm font-medium text-zinc-100">{user?.name ?? "User"}</span>
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
                                variant={isActive ? "primary" : "ghost"}
                                size="sm"
                                onClick={() => onNavigate?.(item.path)}
                            >
                                {item.label}
                            </Button>
                        );
                    })}

                    <Button variant="outline" size="sm" onClick={onLogout} className="border-zinc-700/90 text-zinc-300 ml-2">
                        Logout
                    </Button>
                </nav>
            </div>
        </header>
    );
}

