import { useState } from "react";
import { ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";

export default function Login() {
    const { login } = useAuth();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");
        setIsSubmitting(true);

        try {
            await login({ email, password });
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                setError("Invalid email or password.");
            } else {
                setError("Unable to login right now. Please try again.");
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="h-screen flex items-center justify-center bg-gray-900">
            <form className="bg-gray-800 p-8 rounded-xl w-96" onSubmit={handleSubmit}>
                <h1 className="text-white text-xl mb-4">Login</h1>

                <input
                    className="w-full mb-3 p-2 rounded bg-gray-700 text-white"
                    placeholder="Email"
                    type="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    required
                />

                <input
                    type="password"
                    className="w-full mb-4 p-2 rounded bg-gray-700 text-white"
                    placeholder="Password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    required
                />

                {error ? <p className="text-sm text-red-400 mb-3">{error}</p> : null}

                <button
                    type="submit"
                    disabled={isSubmitting}
                    className="w-full bg-blue-500 p-2 rounded text-white disabled:opacity-60"
                >
                    {isSubmitting ? "Signing in..." : "Login"}
                </button>
            </form>
        </div>
    );
}