import { useCallback, useEffect, useMemo, useState } from "react";
import { fetchMyCourses } from "../api/courses";
import {
    fetchCourseEnrollments,
    fetchMyEnrollments,
    mapEnrollmentError,
    unenrollFromCourse,
} from "../api/enrollments";
import EnrollmentList from "../components/enrollments/EnrollmentList";
import { useAuth } from "../auth/AuthContext";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import EmptyState from "../components/ui/EmptyState";
import PageHeader from "../components/ui/PageHeader";
import Skeleton from "../components/ui/Skeleton";
import { useToast } from "../components/ui/ToastContext";
import { ShieldAlert } from "lucide-react";

const PAGE_SIZE = 10;

function getCourseIdFromSearch(search) {
    const params = new URLSearchParams(search || "");
    const courseId = params.get("courseId");
    return courseId ? String(courseId) : "";
}

export default function EnrollmentsPage({ onNavigate, search = "" }) {
    const { currentUser } = useAuth();
    const role = currentUser?.role ?? "STUDENT";
    const toast = useToast();

    const [page, setPage] = useState(0);
    const [enrollmentsPage, setEnrollmentsPage] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isActionLoading, setIsActionLoading] = useState(false);

    const [instructorCourses, setInstructorCourses] = useState([]);
    const initialCourseIdFromSearch = useMemo(() => getCourseIdFromSearch(search), [search]);
    const [selectedCourseId, setSelectedCourseId] = useState(() => initialCourseIdFromSearch);

    const query = useMemo(() => ({ page, size: PAGE_SIZE }), [page]);

    const loadInstructorCourses = useCallback(async () => {
        if (role !== "INSTRUCTOR") return;

        try {
            const response = await fetchMyCourses({ page: 0, size: 50 });
            const courses = response?.content ?? [];
            setInstructorCourses(courses);

            const preferredFromUrl = initialCourseIdFromSearch
                ? courses.find((course) => String(course.id) === initialCourseIdFromSearch)
                : null;

            setSelectedCourseId((current) => {
                if (current && courses.some((course) => String(course.id) === String(current))) return String(current);
                if (preferredFromUrl) return String(preferredFromUrl.id);
                if (courses.length > 0) return String(courses[0].id);
                return "";
            });
        } catch (loadError) {
            toast.error("Failed to load courses", mapEnrollmentError(loadError));
        }
    }, [role, initialCourseIdFromSearch, toast]);

    const loadEnrollments = useCallback(async () => {
        setIsLoading(true);
        try {
            let data;
            if (role === "INSTRUCTOR") {
                if (!selectedCourseId) {
                    setEnrollmentsPage({ content: [], pageNumber: 0, totalPages: 0, hasNext: false, hasPrevious: false });
                    return;
                }
                data = await fetchCourseEnrollments(selectedCourseId, query);
            } else {
                data = await fetchMyEnrollments(query);
            }
            setEnrollmentsPage(data);
        } catch (loadError) {
            toast.error("Failed to load enrollments", mapEnrollmentError(loadError));
        } finally {
            setIsLoading(false);
        }
    }, [role, query, selectedCourseId, toast]);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect -- Data fetching on mount is a standard pattern in plain React.
        loadInstructorCourses();
    }, [loadInstructorCourses]);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect -- Data fetching on mount is a standard pattern in plain React.
        loadEnrollments();
    }, [loadEnrollments]);

    async function handleUnenroll(enrollment) {
        setIsActionLoading(true);
        try {
            await unenrollFromCourse(enrollment.courseId);
            toast.success("Success", "Unenrolled successfully.");
            setEnrollmentsPage((prev) => {
                if (!prev) return prev;
                return {
                    ...prev,
                    content: (prev.content ?? []).filter((item) => item.id !== enrollment.id),
                    numberOfElements: Math.max((prev.numberOfElements ?? 1) - 1, 0),
                    totalElements: Math.max((prev.totalElements ?? 1) - 1, 0),
                };
            });
        } catch (actionError) {
            toast.error("Failed to unenroll", mapEnrollmentError(actionError));
        } finally {
            setIsActionLoading(false);
        }
    }

    if (role === "ADMIN") {
        return (
            <div className="min-h-screen bg-zinc-950 px-4 py-8 text-zinc-100">
                <main className="mx-auto w-full max-w-6xl space-y-6">
                    <PageHeader
                        eyebrow="Administration"
                        title="Enrollments"
                        subtitle="Enrollment detail views are not enabled for administrators yet."
                        actions={(
                            <Button type="button" variant="outline" size="sm" onClick={() => onNavigate?.("/dashboard")}>Dashboard</Button>
                        )}
                    />

                    <EmptyState
                        icon={ShieldAlert}
                        title="Admin enrollment view unavailable"
                        description="You can still navigate to the dashboard or manage courses, but the admin enrollment view is not exposed yet."
                        actionLabel="Back to dashboard"
                        actionVariant="outline"
                        onAction={() => onNavigate?.("/dashboard")}
                    />
                </main>
            </div>
        );
    }

    const enrollments = enrollmentsPage?.content ?? [];

    return (
        <div className="min-h-screen bg-zinc-950 text-zinc-100">
            <main className="mx-auto w-full max-w-6xl space-y-8 px-4 py-8">
                <PageHeader
                    eyebrow={role === "INSTRUCTOR" ? "Instructor workspace" : "Student workspace"}
                    title="Enrollments"
                    subtitle={role === "INSTRUCTOR"
                        ? "Inspect student enrollments for the courses you own."
                        : "Review the courses you are enrolled in and manage your active enrollments."}
                    actions={(
                        <div className="flex gap-2">
                            <Button type="button" variant="outline" size="sm" onClick={() => onNavigate?.("/dashboard")}>Dashboard</Button>
                            <Button type="button" variant="outline" size="sm" onClick={() => onNavigate?.("/courses")}>Courses</Button>
                        </div>
                    )}
                />

                {role === "INSTRUCTOR" ? (
                    <Card className="space-y-4">
                        <div>
                            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-zinc-500">Course selector</p>
                            <h2 className="mt-2 text-lg font-semibold tracking-tight text-zinc-50">Choose a course to inspect</h2>
                        </div>

                        <label className="block max-w-sm text-sm">
                            <span className="mb-1.5 block font-medium text-zinc-400">Select course</span>
                            <select
                                className="w-full rounded-lg border border-zinc-700 bg-zinc-950/80 px-3 py-2.5 text-zinc-100 outline-none transition focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50"
                                value={selectedCourseId}
                                onChange={(event) => {
                                    setSelectedCourseId(event.target.value);
                                    setPage(0);
                                }}
                            >
                                {instructorCourses.length === 0 ? <option value="">No courses found</option> : null}
                                {instructorCourses.map((course) => (
                                    <option key={course.id} value={course.id}>{course.title}</option>
                                ))}
                            </select>
                        </label>
                    </Card>
                ) : null}

                {isLoading ? (
                    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                        {Array.from({ length: 3 }).map((_, i) => (
                            <Skeleton key={i} className="h-40 w-full rounded-2xl" />
                        ))}
                    </div>
                ) : (
                    <EnrollmentList
                        enrollments={enrollments}
                        mode={role === "INSTRUCTOR" ? "instructor" : "student"}
                        onUnenroll={handleUnenroll}
                        isActionLoading={isActionLoading}
                        emptyState={role === "INSTRUCTOR"
                            ? {
                                title: "No enrollments yet",
                                description: "No students have enrolled in the selected course yet.",
                            }
                            : {
                                title: "No enrollments yet",
                                description: "You are not enrolled in any courses yet.",
                                actionLabel: "Browse courses",
                                actionVariant: "primary",
                                onAction: () => onNavigate?.("/courses"),
                            }}
                    />
                )}

                {!isLoading && enrollmentsPage && enrollmentsPage.totalPages > 1 ? (
                    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-zinc-800/80 bg-zinc-900/50 px-4 py-3 text-sm text-zinc-400">
                        <div>
                            Page {enrollmentsPage.pageNumber + 1} of {Math.max(enrollmentsPage.totalPages, 1)}
                        </div>
                        <div className="flex items-center gap-2">
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                                disabled={enrollmentsPage.hasPrevious === false && page === 0}
                            >
                                Previous
                            </Button>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setPage((prev) => prev + 1)}
                                disabled={enrollmentsPage.hasNext === false}
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
