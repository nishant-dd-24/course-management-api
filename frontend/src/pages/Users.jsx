import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "../api/client";
import Badge from "../components/ui/Badge";
import Button from "../components/ui/Button";
import EmptyState from "../components/ui/EmptyState";
import PageHeader from "../components/ui/PageHeader";
import Table from "../components/ui/Table";
import Skeleton from "../components/ui/Skeleton";
import { useToast } from "../components/ui/ToastContext";
import { Users as UsersIcon, UserX } from "lucide-react";

const PAGE_SIZE = 10;

function buildQuery(page) {
    return `?page=${page}&size=${PAGE_SIZE}`;
}

function roleVariant(role) {
    return role?.toLowerCase() ?? "neutral";
}

export default function Users({ onNavigate }) {
    const toast = useToast();

    const [page, setPage] = useState(0);
    const [usersPage, setUsersPage] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [actionLoadingId, setActionLoadingId] = useState(null);

    useEffect(() => {
        let cancelled = false;

        async function init() {
            setIsLoading(true);

            try {
                const data = await apiFetch(`/users${buildQuery(page)}`);
                if (!cancelled) setUsersPage(data);
            } catch (fetchError) {
                if (!cancelled) {
                    toast.error("Failed to load users", fetchError?.message || "Unable to load users right now.");
                }
            } finally {
                if (!cancelled) setIsLoading(false);
            }
        }

        init();
        return () => { cancelled = true; };
    }, [page, toast]);

    const users = useMemo(() => usersPage?.content ?? [], [usersPage]);

    async function handleDeactivate(user) {
        if (!user?.id) return;
        setActionLoadingId(user.id);

        try {
            await apiFetch(`/users/${user.id}`, { method: "DELETE" });
            setUsersPage((prev) => {
                if (!prev) return prev;
                return {
                    ...prev,
                    content: (prev.content ?? []).map((item) => (item.id === user.id ? { ...item, isActive: false } : item)),
                };
            });
            toast.success("User deactivated", `${user.name} has been deactivated.`);
        } catch (deactivateError) {
            toast.error("Deactivation failed", deactivateError?.message || "Unable to deactivate user.");
        } finally {
            setActionLoadingId(null);
        }
    }

    async function handleRoleChange(user, newRole) {
        if (!user?.id || user.role === newRole) return;
        setActionLoadingId(user.id);

        try {
            await apiFetch(`/users/${user.id}`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ role: newRole }),
            });

            setUsersPage((prev) => {
                if (!prev) return prev;
                return {
                    ...prev,
                    content: (prev.content ?? []).map((item) => (item.id === user.id ? { ...item, role: newRole } : item)),
                };
            });
            toast.success("Role updated", `${user.name} is now ${newRole}.`);
        } catch (roleError) {
            toast.error("Update failed", roleError?.message || "Unable to update role.");
        } finally {
            setActionLoadingId(null);
        }
    }

    return (
        <div className="min-h-screen bg-zinc-950 text-zinc-100">
            <main className="mx-auto w-full max-w-6xl space-y-8 px-4 py-8">
                <PageHeader
                    eyebrow="Administration"
                    title="Manage Users"
                    subtitle="Review accounts, adjust roles, and deactivate access without leaving the dashboard."
                    actions={(
                        <div className="flex gap-2">
                            <Button variant="outline" size="sm" onClick={() => onNavigate?.("/dashboard")}>Dashboard</Button>
                            <Button variant="outline" size="sm" onClick={() => onNavigate?.("/courses")}>Courses</Button>
                        </div>
                    )}
                />

                {isLoading ? (
                    <div className="space-y-4">
                        <Skeleton className="h-12 w-full rounded-xl" />
                        <Skeleton className="h-16 w-full rounded-xl" />
                        <Skeleton className="h-16 w-full rounded-xl" />
                        <Skeleton className="h-16 w-full rounded-xl" />
                    </div>
                ) : null}

                {!isLoading && users.length === 0 ? (
                    <EmptyState
                        icon={UsersIcon}
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
                                <th scope="col" className="w-[20%] text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map((user) => (
                                <tr key={user.id} className="group">
                                    <td>
                                        <div className="flex items-center gap-3">
                                            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-zinc-800 text-xs font-semibold text-zinc-300 ring-1 ring-zinc-700/50">
                                                {user.name.charAt(0).toUpperCase()}
                                            </div>
                                            <span className="font-medium text-zinc-100">{user.name}</span>
                                        </div>
                                    </td>
                                    <td className="text-zinc-400 align-middle">{user.email}</td>
                                    <td className="align-middle">
                                        <div className="flex flex-col gap-2 max-w-[140px]">
                                            <Badge variant={roleVariant(user.role)} className="w-fit">{user.role}</Badge>
                                            <select
                                                value={user.role}
                                                onChange={(event) => handleRoleChange(user, event.target.value)}
                                                disabled={actionLoadingId === user.id}
                                                className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-1.5 text-xs text-zinc-300 outline-none transition focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50 disabled:cursor-not-allowed disabled:opacity-60"
                                            >
                                                <option value="STUDENT">STUDENT</option>
                                                <option value="INSTRUCTOR">INSTRUCTOR</option>
                                                <option value="ADMIN">ADMIN</option>
                                            </select>
                                        </div>
                                    </td>
                                    <td className="align-middle">
                                        <Badge variant={user.isActive ? "active" : "inactive"}>
                                            {user.isActive ? "Active" : "Inactive"}
                                        </Badge>
                                    </td>
                                    <td className="align-middle text-right">
                                        <Button
                                            type="button"
                                            onClick={() => handleDeactivate(user)}
                                            disabled={actionLoadingId === user.id || !user.isActive}
                                            variant="ghost"
                                            size="sm"
                                            className={user.isActive ? "text-red-400 hover:text-red-300 hover:bg-red-500/10" : "opacity-0"}
                                            title="Deactivate User"
                                        >
                                            <UserX className="h-4 w-4" />
                                        </Button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </Table>
                ) : null}

                {!isLoading && usersPage && usersPage.totalPages > 1 ? (
                    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-zinc-800/80 bg-zinc-900/50 px-4 py-3 text-sm text-zinc-400">
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