export default function AuthShell({
    variant = "login",
    title,
    subtitle,
    children,
    footer,
}) {
    const accent = variant === "register" ? "from-emerald-500 to-teal-500" : "from-blue-500 to-indigo-500";
    const highlights = variant === "register"
        ? ["Fast onboarding", "Role-aware access", "Clean course management"]
        : ["Secure JWT sessions", "Responsive dashboard", "Role-aware workflows"];

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100">
            <div className="mx-auto grid min-h-screen w-full max-w-6xl items-center gap-6 px-4 py-8 lg:grid-cols-[1.05fr_0.95fr]">
                <section className="relative overflow-hidden rounded-3xl border border-slate-800/80 bg-slate-950/80 p-8 shadow-[0_24px_80px_-40px_rgba(15,23,42,0.9)]">
                    <div className="absolute inset-0 bg-gradient-to-br from-blue-500/10 via-transparent to-indigo-500/10" />
                    <div className={`absolute right-8 top-8 h-28 w-28 rounded-full bg-gradient-to-br ${accent} opacity-20 blur-3xl`} />

                    <div className="relative space-y-6">
                        <p className="text-sm font-semibold uppercase tracking-[0.28em] text-slate-500">Course Management</p>
                        <div className={`h-1.5 w-24 rounded-full bg-gradient-to-r ${accent}`} />
                        <h1 className="max-w-xl text-4xl font-semibold tracking-tight text-slate-50 lg:text-5xl">{title}</h1>
                        <p className="max-w-xl text-base leading-7 text-slate-300">{subtitle}</p>

                        <div className="grid gap-3 pt-2 sm:grid-cols-3">
                            {highlights.map((item) => (
                                <div key={item} className="rounded-2xl border border-slate-800/80 bg-slate-900/70 px-4 py-3 text-sm text-slate-300">
                                    {item}
                                </div>
                            ))}
                        </div>
                    </div>
                </section>

                <section className="rounded-3xl border border-slate-800/80 bg-slate-900/95 p-8 shadow-[0_24px_80px_-40px_rgba(15,23,42,0.9)]">
                    <div className="space-y-6">
                        {children}
                        {footer ? <div className="text-sm text-slate-300">{footer}</div> : null}
                    </div>
                </section>
            </div>
        </div>
    );
}

