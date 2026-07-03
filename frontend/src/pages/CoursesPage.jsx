import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiError } from "../api/client";
import {
    createCourse,
    activateCourse,
    fetchActiveCourses,
    fetchAllCourses,
    fetchMyCourses,
    deactivateCourse,
    updateCourse,
} from "../api/courses";
import { fetchMyEnrollments, mapEnrollmentError } from "../api/enrollments";
import CourseForm from "../components/courses/CourseForm";
import CourseList from "../components/courses/CourseList";
import { useAuth } from "../auth/AuthContext";
import Button from "../components/ui/Button";
import PageHeader from "../components/ui/PageHeader";

const PAGE_SIZE = 6;
const ENROLLMENT_SYNC_PAGE_SIZE = 50;

function mapApiError(error) {
    if (error instanceof ApiError && error.status === 403) {
        return "You do not have permission for this action.";
    }

    if (error instanceof ApiError && error.status === 400) {
        return error.message || "Please review your input and try again.";
    }

    if (error instanceof ApiError && error.status === 404) {
        return "Course not found. It may have been changed by another user.";
    }

    return "Unable to complete the request right now.";
}

export default function CoursesPage({ onNavigate }) {
    const { currentUser, logout } = useAuth();
    const role = currentUser?.role ?? "STUDENT";

    const [coursesPage, setCoursesPage] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");
    const [page, setPage] = useState(0);
    const [editor, setEditor] = useState(null);
    const [submitError, setSubmitError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [notice, setNotice] = useState("");
    const [enrolledCourseIds, setEnrolledCourseIds] = useState(() => new Set());

    const query = useMemo(() => ({ page, size: PAGE_SIZE }), [page]);

    const loadCourses = useCallback(async () => {
        setIsLoading(true);
        setError("");

        try {
            let data;

            if (role === "INSTRUCTOR") {
                data = await fetchMyCourses(query);
            } else if (role === "ADMIN") {
                data = await fetchAllCourses(query);
            } else {
                data = await fetchActiveCourses(query);
            }

            setCoursesPage(data);
        } catch (loadError) {
            setError(mapApiError(loadError));
        } finally {
            setIsLoading(false);
        }
    }, [role, query]);

    const syncStudentEnrollments = useCallback(async () => {
        if (role !== "STUDENT") {
            return;
        }

        try {
            const enrollmentPage = await fetchMyEnrollments({ page: 0, size: ENROLLMENT_SYNC_PAGE_SIZE });
            const next = new Set((enrollmentPage?.content ?? []).map((item) => item.courseId));
            setEnrolledCourseIds(next);
        } catch (syncError) {
            setError(mapEnrollmentError(syncError));
        }
    }, [role]);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        loadCourses();
    }, [loadCourses]);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        syncStudentEnrollments();
    }, [syncStudentEnrollments]);

    async function handleCreateCourse(payload) {
        setIsSubmitting(true);
        setSubmitError("");

        try {
            await createCourse(payload);
            setEditor(null);
            setNotice("Course created successfully.");
            await loadCourses();
        } catch (submitErr) {
            setSubmitError(mapApiError(submitErr));
        } finally {
            setIsSubmitting(false);
        }
    }

    async function handleUpdateCourse(payload) {
        if (!editor?.course?.id) {
            return;
        }

        setIsSubmitting(true);
        setSubmitError("");

        try {
            await updateCourse(editor.course.id, payload);
            setEditor(null);
            setNotice("Course updated successfully.");
            await loadCourses();
        } catch (submitErr) {
            setSubmitError(mapApiError(submitErr));
        } finally {
            setIsSubmitting(false);
        }
    }

    async function handleToggleCourseStatus(course) {
        const nextAction = course.isActive ? "Deactivate" : "Activate";
        const confirmed = window.confirm(`${nextAction} "${course.title}"?`);

        if (!confirmed) {
            return;
        }

        setNotice("");

        try {
            const updatedCourse = course.isActive
                ? await deactivateCourse(course.id).then(() => ({ ...course, isActive: false }))
                : await activateCourse(course.id).then(() => ({ ...course, isActive: true }));
            setCoursesPage((prev) => {
                if (!prev) {
                    return prev;
                }

                return {
                    ...prev,
                    content: (prev.content ?? []).map((item) => (item.id === course.id ? { ...item, ...updatedCourse } : item)),
                };
            });
            setNotice(`Course ${updatedCourse.isActive ? "activated" : "deactivated"}.`);
        } catch (actionErr) {
            setError(mapApiError(actionErr));
        }
    }

    function handleEnrollSuccess(course) {
        setNotice(`Enrolled in "${course.title}" successfully.`);
        setError("");
        setEnrolledCourseIds((prev) => {
            const next = new Set(prev);
            next.add(course.id);
            return next;
        });

        setCoursesPage((prev) => {
            if (!prev) {
                return prev;
            }

            return {
                ...prev,
                content: (prev.content ?? []).map((item) => {
                    if (item.id !== course.id) {
                        return item;
                    }

                    return {
                        ...item,
                        availableSeats: Math.max((item.availableSeats ?? 0) - 1, 0),
                    };
                }),
            };
        });
    }

    function handleEnrollError(message) {
        setError(message);
        setNotice("");
    }

    const handleOpenEnrollments = useCallback((course) => {
        if (!course?.id) {
            return;
        }

        onNavigate?.(`/enrollments?courseId=${encodeURIComponent(String(course.id))}`);
    }, [onNavigate]);

    const courses = coursesPage?.content ?? [];

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100">
            <main className="mx-auto w-full max-w-6xl space-y-6 px-4 py-8">
                <PageHeader
                    eyebrow={role === "INSTRUCTOR" ? "Instructor workspace" : role === "ADMIN" ? "Administration" : "Student workspace"}
                    title="Courses"
                    subtitle={role === "INSTRUCTOR"
                        ? "Create, edit, activate, and deactivate the courses you own."
                        : role === "ADMIN"
                            ? "Review the full course catalog across the platform."
                            : "Browse active courses and enroll in the ones that fit your goals."}
                    actions={(
                        <>
                            {role === "INSTRUCTOR" ? (
                                <Button
                                    type="button"
                                    variant="primary"
                                    size="sm"
                                    onClick={() => {
                                        setEditor({ mode: "create", course: null });
                                        setSubmitError("");
                                        setNotice("");
                                    }}
                                >
                                    Create Course
                                </Button>
                            ) : null}
                            <Button type="button" variant="outline" size="sm" onClick={() => onNavigate?.("/dashboard")}>Dashboard</Button>
                            <Button type="button" variant="outline" size="sm" onClick={logout}>Logout</Button>
                        </>
                    )}
                />

                {editor ? (
                <section className="space-y-4">
                    <CourseForm
                        key={`${editor.mode}-${editor.course?.id ?? "new"}`}
                        mode={editor.mode}
                        initialValues={editor.course}
                        isSubmitting={isSubmitting}
                        submitError={submitError}
                        onSubmit={editor.mode === "edit" ? handleUpdateCourse : handleCreateCourse}
                        onCancel={() => {
                            setEditor(null);
                            setSubmitError("");
                        }}
                    />
                </section>
                ) : null}

                {notice ? (
                    <div className="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-200">
                        {notice}
                    </div>
                ) : null}

                {error ? (
                    <div className="rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
                        {error}
                    </div>
                ) : null}

                {isLoading ? (
                    <div className="rounded-2xl border border-slate-800/80 bg-slate-900/95 px-6 py-10 text-sm text-slate-300">
                        Loading courses...
                    </div>
                ) : null}

                {!isLoading ? (
                    <CourseList
                        courses={courses}
                        role={role}
                        onEdit={(course) => {
                            setEditor({ mode: "edit", course });
                            setSubmitError("");
                            setNotice("");
                        }}
                        onDeactivate={handleToggleCourseStatus}
                        onOpenEnrollments={handleOpenEnrollments}
                        onEnroll={handleEnrollSuccess}
                        onEnrollError={handleEnrollError}
                        enrolledCourseIds={enrolledCourseIds}
                        emptyState={role === "INSTRUCTOR"
                            ? {
                                icon: "📝",
                                title: "No courses yet",
                                description: "Create your first course to start managing enrollments and course content.",
                                actionLabel: "Create Course",
                                actionVariant: "primary",
                                onAction: () => {
                                    setEditor({ mode: "create", course: null });
                                    setSubmitError("");
                                    setNotice("");
                                },
                            }
                            : role === "STUDENT"
                                ? {
                                    icon: "📚",
                                    title: "No active courses right now",
                                    description: "There are no active courses available for enrollment at the moment.",
                                    actionLabel: "Dashboard",
                                    actionVariant: "outline",
                                    onAction: () => onNavigate?.("/dashboard"),
                                }
                                : {
                                    icon: "🗂️",
                                    title: "No courses found",
                                    description: "There are no courses to display for this view yet.",
                                    actionLabel: "Dashboard",
                                    actionVariant: "outline",
                                    onAction: () => onNavigate?.("/dashboard"),
                                }}
                    />
                ) : null}

                {!isLoading && coursesPage ? (
                    <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-800/80 bg-slate-900/80 px-4 py-3 text-sm text-slate-300">
                        <div>
                            Page {coursesPage.pageNumber + 1} of {Math.max(coursesPage.totalPages, 1)}
                        </div>
                        <div className="flex items-center gap-2">
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                                disabled={coursesPage.hasPrevious === false && page === 0}
                            >
                                Previous
                            </Button>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setPage((prev) => prev + 1)}
                                disabled={coursesPage.hasNext === false}
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
