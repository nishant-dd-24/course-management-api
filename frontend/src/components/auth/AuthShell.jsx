export default function AuthShell({
    variant = "login",
    title,
    subtitle,
    children,
    footer,
}) {
    const isRegister = variant === "register";

    return (
        <div className="flex min-h-screen w-full bg-zinc-950 text-zinc-100">
            {/* Left side form */}
            <div className="flex w-full flex-col justify-center px-4 py-12 sm:px-6 lg:flex-none lg:w-1/2 lg:px-20 xl:px-24">
                <div className="mx-auto w-full max-w-sm lg:w-96">
                    <div className="mb-8">
                        <div className="flex h-12 w-12 items-center justify-center">
                            <img src="/logo.png" alt="Course Management Logo" className="h-full w-full object-contain" />
                        </div>
                        <h2 className="mt-6 text-3xl font-semibold tracking-tighter text-zinc-50">{title}</h2>
                        <p className="mt-2 text-sm leading-6 text-zinc-400">{subtitle}</p>
                    </div>

                    <div className="mt-10">
                        {children}
                        {footer ? <div className="mt-6 text-sm text-zinc-400">{footer}</div> : null}
                    </div>
                </div>
            </div>
            
            {/* Right side visual */}
            <div className="relative hidden w-0 flex-1 lg:block overflow-hidden border-l border-zinc-800/80 bg-zinc-900/50">
                <div className="absolute inset-0 bg-gradient-to-br from-blue-500/5 via-zinc-900/50 to-indigo-500/5" />
                
                {/* Decorative mesh/blur effect */}
                <div className="absolute top-[20%] left-[20%] w-[500px] h-[500px] bg-blue-500/10 rounded-full blur-3xl opacity-50" />
                <div className="absolute bottom-[20%] right-[20%] w-[400px] h-[400px] bg-indigo-500/10 rounded-full blur-3xl opacity-50" />

                <div className="absolute inset-0 flex items-center justify-center p-12">
                    <div className="max-w-md space-y-6 text-center">
                        <h3 className="text-3xl font-semibold tracking-tighter text-zinc-100">
                            {isRegister ? "Join the platform" : "Manage your learning"}
                        </h3>
                        <p className="text-zinc-400">
                            {isRegister 
                                ? "Sign up to unlock course enrollment, track your progress, and manage your educational journey." 
                                : "Securely sign in to access your dashboard, active courses, and platform tools."}
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}
