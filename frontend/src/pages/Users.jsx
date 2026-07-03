import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import Badge from "../components/ui/Badge";
import Button from "../components/ui/Button";
import EmptyState from "../components/ui/EmptyState";
import PageHeader from "../components/ui/PageHeader";
import Table from "../components/ui/Table";

const PAGE_SIZE = 10;

function buildQuery(page) {
    return `?page=${page}&size=${PAGE_SIZE}`;
}

function roleVariant(role) {
    return role?.toLowerCase() ?? "neutral";
}

export default function Users({ onNavigate }) {
    const { logout } = useAuth();

    const [page, setPage] = useState(0);
    const [usersPage, setUsersPage] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");
    const [actionLoadingId, setActionLoadingId] = useState(null);

    useEffect(() => {
        let cancelled = false;

        async function init() {
            setIsLoading(true);
            setError("");

            try {
                const data = await apiFetch(`/users${buildQuery(page)}`);

                if (!cancelled) {
                    setUsersPage(data);
                }
            } catch (fetchError) {
                if (!cancelled) {
                    setError((fetchError && fetchError.message) || "Unable to load users right now.");
                }
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        }

        init();

        return () => {
            cancelled = true;
        };
    }, [page]);

    const users = useMemo(() => usersPage?.content ?? [], [usersPage]);

    async function handleDeactivate(user) {
        if (!user?.id) {
            return;
        }

        setActionLoadingId(user.id);
        setError("");

        try {
            await apiFetch(`/users/${user.id}`, { method: "DELETE" });

            setUsersPage((prev) => {
                if (!prev) {
                    return prev;
                }

                return {
                    ...prev,
                    content: (prev.content ?? []).map((item) => (item.id === user.id ? { ...item, isActive: false } : item)),
                };
            });
        } catch (deactivateError) {
            setError((deactivateError && deactivateError.message) || "Unable to deactivate user.");
        } finally {
            setActionLoadingId(null);
        }
    }

    async function handleRoleChange(user, newRole) {
        if (!user?.id || user.role === newRole) {
            return;
        }

        setActionLoadingId(user.id);
        setError("");

        try {
            await apiFetch(`/users/${user.id}`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ role: newRole }),
            });

            setUsersPage((prev) => {
                if (!prev) {
                    return prev;
                }

                return {
                    ...prev,
                    content: (prev.content ?? []).map((item) => (item.id === user.id ? { ...item, role: newRole } : item)),
                };
            });
        } catch (roleError) {
            setError((roleError && roleError.message) || "Unable to update role.");
        } finally {
            setActionLoadingId(null);
        }
    }

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100">
            <main className="mx-auto w-full max-w-6xl space-y-6 px-4 py-8">
                <PageHeader
                    eyebrow="Administration"
                    title="Manage Users"
                    subtitle="Review accounts, adjust roles, and deactivate access without leaving the dashboard."
                    actions={(
                        <>
                            <Button variant="outline" size="sm" onClick={() => onNavigate?.("/dashboard")}>Dashboard</Button>
                            <Button variant="outline" size="sm" onClick={() => onNavigate?.("/courses")}>Courses</Button>
                            <Button variant="outline" size="sm" onClick={logout}>Logout</Button>
                        </>
                    )}
                />

                {error ? (
                    <div className="rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
                        {error}
                    </div>
                ) : null}

                {isLoading ? (
                    <div className="rounded-2xl border border-slate-800/80 bg-slate-900/95 px-6 py-10 text-sm text-slate-300">
                        Loading users...
                    </div>
                ) : null}

                {!isLoading && users.length === 0 ? (
                    <EmptyState
                        icon="👥"
                        title="No users found"
                        description="There are no user records to display on this page yet."
                        actionLabel="Back to dashboard"
                        actionVariant="outline"
                        onAction={() => onNavigate?.("/dashboard")}
                    />
                ) : null}

                {!isLoading && users.length > 0 ? (
                    <Table>
                        <thead>
                            <tr>
                                <th scope="col" className="w-[18%]">Name</th>
                                <th scope="col" className="w-[28%]">Email</th>
                                <th scope="col" className="w-[18%]">Role</th>
                                <th scope="col" className="w-[16%]">Status</th>
                                <th scope="col" className="w-[20%]">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map((user) => (
                                <tr key={user.id}>
                                    <td className="font-medium text-slate-100">{user.name}</td>
                                    <td className="text-slate-300">{user.email}</td>
                                    <td>
                                        <div className="flex flex-col gap-2">
                                            <Badge variant={roleVariant(user.role)}>{user.role}</Badge>
                                            <select
                                                value={user.role}
                                                onChange={(event) => handleRoleChange(user, event.target.value)}
                                                disabled={actionLoadingId === user.id}
                                                className="w-full rounded-xl border border-slate-700 bg-slate-950/80 px-3 py-2 text-sm text-slate-100 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 disabled:cursor-not-allowed disabled:opacity-60"
                                            >
                                                <option value="STUDENT">STUDENT</option>
                                                <option value="INSTRUCTOR">INSTRUCTOR</option>
                                                <option value="ADMIN">ADMIN</option>
                                            </select>
                                        </div>
                                    </td>
                                    <td>
                                        <Badge variant={user.isActive ? "active" : "inactive"}>
                                            {user.isActive ? "Active" : "Inactive"}
                                        </Badge>
                                    </td>
                                    <td>
                                        <div className="flex flex-wrap gap-2">
                                            <Button
                                                type="button"
                                                onClick={() => handleDeactivate(user)}
                                                disabled={actionLoadingId === user.id || !user.isActive}
                                                variant="danger"
                                                size="sm"
                                            >
                                                Deactivate
                                            </Button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </Table>
                ) : null}

                {!isLoading && usersPage ? (
                    <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-800/80 bg-slate-900/80 px-4 py-3 text-sm text-slate-300">
                        <div>
                            Page {usersPage.pageNumber + 1} of {Math.max(usersPage.totalPages, 1)}
                        </div>
                        <div className="flex items-center gap-2">
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                                disabled={usersPage.hasPrevious === false && page === 0}
                            >
                                Previous
                            </Button>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setPage((prev) => prev + 1)}
                                disabled={usersPage.hasNext === false}
                            >
                                Next
                            </Button>
                        </div>
                    </div>
                ) : null}
            </main>
        </div>
    );
}