import { useMemo, useState } from "react";
import { ApiError } from "../api/client";
import { register as registerUser } from "../services/authService";
import AuthShell from "../components/auth/AuthShell";
import Button from "../components/ui/Button";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validate(values) {
    const errors = {};

    if (!values.name.trim()) {
        errors.name = "Name is required.";
    } else if (values.name.trim().length > 50) {
        errors.name = "Name must be 50 characters or fewer.";
    }

    if (!values.email.trim()) {
        errors.email = "Email is required.";
    } else if (!EMAIL_REGEX.test(values.email.trim())) {
        errors.email = "Enter a valid email address.";
    }

    if (!values.password) {
        errors.password = "Password is required.";
    } else if (values.password.length < 8) {
        errors.password = "Password must be at least 8 characters.";
    } else if (!/[A-Za-z]/.test(values.password) || !/\d/.test(values.password)) {
        errors.password = "Use at least one letter and one number.";
    }

    if (!values.confirmPassword) {
        errors.confirmPassword = "Please confirm your password.";
    } else if (values.password !== values.confirmPassword) {
        errors.confirmPassword = "Passwords do not match.";
    }

    return errors;
}

function mapRegisterError(error) {
    if (error instanceof ApiError && error.status === 409) {
        return "An account with this email already exists.";
    }

    if (error instanceof ApiError && error.status === 400) {
        if (typeof error.data === "object" && error.data?.message) {
            return error.data.message;
        }

        return "Please review your details and try again.";
    }

    return "We could not create your account right now. Please try again.";
}

export default function Register({ onNavigate }) {
    const [values, setValues] = useState({
        name: "",
        email: "",
        password: "",
        confirmPassword: "",
    });
    const [fieldErrors, setFieldErrors] = useState({});
    const [apiError, setApiError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    const canSubmit = useMemo(
        () =>
            values.name.trim().length > 0
            && values.email.trim().length > 0
            && values.password.length > 0
            && values.confirmPassword.length > 0
            && !isSubmitting,
        [values, isSubmitting]
    );

    function updateField(field, value) {
        setValues((prev) => ({ ...prev, [field]: value }));
        setFieldErrors((prev) => {
            if (!prev[field]) {
                return prev;
            }

            const next = { ...prev };
            delete next[field];
            return next;
        });
        setApiError("");
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setApiError("");

        const errors = validate(values);
        setFieldErrors(errors);

        if (Object.keys(errors).length > 0) {
            return;
        }

        setIsSubmitting(true);

        try {
            await registerUser({
                name: values.name.trim(),
                email: values.email.trim(),
                password: values.password,
            });

            const loginPath = `/login?registered=1&email=${encodeURIComponent(values.email.trim())}`;
            onNavigate?.(loginPath, { replace: true });
        } catch (error) {
            setApiError(mapRegisterError(error));
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <AuthShell
            variant="register"
            title="Create your account"
            subtitle="Start as a student and unlock course enrollment in minutes."
            footer={(
                <p>
                    Already have an account?{" "}
                    <button
                        type="button"
                        className="text-emerald-300 underline-offset-2 hover:underline"
                        onClick={() => onNavigate?.("/login", { replace: true })}
                    >
                        Login
                    </button>
                </p>
            )}
        >
            <p className="mb-4 text-sm leading-6 text-slate-300">
                Use a strong password to keep your account secure.
            </p>

            {apiError ? <p className="mb-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">{apiError}</p> : null}

            <form className="space-y-5" onSubmit={handleSubmit}>
                <label className="block text-sm">
                    <span className="mb-1.5 block text-sm font-medium text-zinc-300">Name</span>
                    <input
                        className="w-full rounded-lg border border-zinc-700 bg-zinc-950/50 px-4 py-3 text-zinc-100 outline-none transition focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50"
                        placeholder="John Doe"
                        value={values.name}
                        onChange={(event) => updateField("name", event.target.value)}
                        required
                    />
                    {fieldErrors.name ? <span className="mt-1 block text-xs text-red-400">{fieldErrors.name}</span> : null}
                </label>

                <label className="block text-sm">
                    <span className="mb-1.5 block text-sm font-medium text-zinc-300">Email</span>
                    <input
                        className="w-full rounded-lg border border-zinc-700 bg-zinc-950/50 px-4 py-3 text-zinc-100 outline-none transition focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50"
                        placeholder="you@example.com"
                        type="email"
                        value={values.email}
                        onChange={(event) => updateField("email", event.target.value)}
                        required
                    />
                    {fieldErrors.email ? <span className="mt-1 block text-xs text-red-400">{fieldErrors.email}</span> : null}
                </label>

                <label className="block text-sm">
                    <span className="mb-1.5 block text-sm font-medium text-zinc-300">Password</span>
                    <input
                        className="w-full rounded-lg border border-zinc-700 bg-zinc-950/50 px-4 py-3 text-zinc-100 outline-none transition focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50"
                        placeholder="At least 8 characters"
                        type="password"
                        value={values.password}
                        onChange={(event) => updateField("password", event.target.value)}
                        required
                    />
                    {fieldErrors.password ? <span className="mt-1 block text-xs text-red-400">{fieldErrors.password}</span> : null}
                </label>

                <label className="block text-sm">
                    <span className="mb-1.5 block text-sm font-medium text-zinc-300">Confirm password</span>
                    <input
                        className="w-full rounded-lg border border-zinc-700 bg-zinc-950/50 px-4 py-3 text-zinc-100 outline-none transition focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50"
                        placeholder="Re-enter your password"
                        type="password"
                        value={values.confirmPassword}
                        onChange={(event) => updateField("confirmPassword", event.target.value)}
                        required
                    />
                    {fieldErrors.confirmPassword ? <span className="mt-1 block text-xs text-red-400">{fieldErrors.confirmPassword}</span> : null}
                </label>

                <Button
                    type="submit"
                    variant="primary"
                    className="w-full mt-2"
                    disabled={!canSubmit}
                    isLoading={isSubmitting}
                >
                    {isSubmitting ? "Creating account..." : "Create account"}
                </Button>
            </form>
        </AuthShell>
    );
}

