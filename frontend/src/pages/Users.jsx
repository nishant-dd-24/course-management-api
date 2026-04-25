import { useEffect, useState } from "react";
import { apiFetch } from "../api/client";
import { useAuth } from "../auth/AuthContext";

export default function Users() {
    const { logout, currentUser } = useAuth();
    const [users, setUsers] = useState(null);
    const [error, setError] = useState("");
    const [isLoading, setIsLoading] = useState(true);

    async function fetchUsers() {
        setError("");
        setIsLoading(true);

        try {
            const data = await apiFetch("/users/my");
            setUsers(data);
        } catch {
            setError("Unable to load protected data right now.");
        } finally {
            setIsLoading(false);
        }
    }

    useEffect(() => {
        let cancelled = false;

        async function loadInitialUsers() {
            try {
                const data = await apiFetch("/users/my");

                if (!cancelled) {
                    setUsers(data);
                }
            } catch {
                if (!cancelled) {
                    setError("Unable to load protected data right now.");
                }
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        }

        loadInitialUsers();

        return () => {
            cancelled = true;
        };
    }, []);

    return (
        <div className="p-6 text-white bg-gray-900 min-h-screen">
            <div className="flex justify-between mb-4 items-center">
                <div>
                    <h1 className="text-2xl">Users</h1>
                    {currentUser?.email ? (
                        <p className="text-sm text-gray-300">Signed in as {currentUser.email}</p>
                    ) : null}
                </div>
                <button onClick={logout} className="bg-red-500 px-4 py-2 rounded">
                    Logout
                </button>
            </div>

            {isLoading ? <p className="text-gray-300">Loading protected data...</p> : null}
            {error ? <p className="text-sm text-red-400 mb-3">{error}</p> : null}
            {!isLoading && !error ? <pre>{JSON.stringify(users, null, 2)}</pre> : null}

            {error ? (
                <button onClick={fetchUsers} className="mt-3 bg-blue-500 px-4 py-2 rounded">
                    Retry
                </button>
            ) : null}
        </div>
    );
}