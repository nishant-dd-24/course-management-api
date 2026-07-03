import { useMemo, useState } from "react";
import { ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import AuthShell from "../components/auth/AuthShell";
import Button from "../components/ui/Button";

function mapLoginError(error) {
    if (error instanceof ApiError && error.status === 401) {
        return "Incorrect email or password.";
    }

    if (error instanceof ApiError && error.status === 429) {
        return "Too many attempts. Please wait a moment and try again.";
    }

    return "Unable to sign in right now. Please try again.";
}

export default function Login({ onSuccessRedirect = "/dashboard", onNavigate, notice = "" }) {
    const { login } = useAuth();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState("");
    const [showPassword, setShowPassword] = useState(false);

    const canSubmit = useMemo(() => email.trim().length > 0 && password.length > 0 && !isSubmitting, [email, password, isSubmitting]);

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");
        setIsSubmitting(true);

        try {
            await login({ email: email.trim(), password });
            onNavigate?.(onSuccessRedirect, { replace: true });
        } catch (err) {
            setError(mapLoginError(err));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <AuthShell
            variant="login"
            title="Welcome back"
            subtitle="Sign in to continue managing courses, enrollments, and your account."
            footer={(
                <p>
                    Don&apos;t have an account?{" "}
                    <button
                        type="button"
                        className="text-blue-300 underline-offset-2 hover:underline"
                        onClick={() => onNavigate?.("/register", { replace: true })}
                    >
                        Register
                    </button>
                </p>
            )}
        >
            {notice ? <p className="mb-4 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-200">{notice}</p> : null}
            {error ? <p className="mb-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">{error}</p> : null}

            <form className="space-y-5" onSubmit={handleSubmit}>
                <label className="block text-sm">
                    <span className="mb-1.5 block text-sm font-medium text-slate-300">Email</span>
                    <input
                        className="w-full rounded-xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                        placeholder="you@example.com"
                        type="email"
                        value={email}
                        onChange={(event) => {
                            setEmail(event.target.value);
                            setError("");
                        }}
                        required
                    />
                </label>

                <label className="block text-sm">
                    <span className="mb-1.5 block text-sm font-medium text-slate-300">Password</span>
                    <div className="flex gap-2">
                        <input
                            type={showPassword ? "text" : "password"}
                            className="w-full rounded-xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                            placeholder="Enter your password"
                            value={password}
                            onChange={(event) => {
                                setPassword(event.target.value);
                                setError("");
                            }}
                            required
                        />
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => setShowPassword((prev) => !prev)}
                        >
                            {showPassword ? "Hide" : "Show"}
                        </Button>
                    </div>
                </label>

                <Button
                    type="submit"
                    variant="primary"
                    className="w-full"
                    disabled={!canSubmit}
                >
                    {isSubmitting ? "Signing in..." : "Sign in"}
                </Button>
            </form>
        </AuthShell>
    );
}