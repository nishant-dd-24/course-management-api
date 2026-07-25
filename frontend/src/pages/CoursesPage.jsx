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
import Modal from "../components/ui/Modal";
import Skeleton from "../components/ui/Skeleton";
import { useToast } from "../components/ui/ToastContext";
import { Plus } from "lucide-react";

const PAGE_SIZE = 6;
const ENROLLMENT_SYNC_PAGE_SIZE = 50;

function mapApiError(error) {
    if (error instanceof ApiError && error.status === 403) return "You do not have permission for this action.";
    if (error instanceof ApiError && error.status === 400) return error.message || "Please review your input and try again.";
    if (error instanceof ApiError && error.status === 404) return "Course not found. It may have been changed by another user.";
    return "Unable to complete the request right now.";
}

export default function CoursesPage({ onNavigate }) {
    const { currentUser } = useAuth();
    const role = currentUser?.role ?? "STUDENT";
    const toast = useToast();

    const [coursesPage, setCoursesPage] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [editor, setEditor] = useState(null);
    const [submitError, setSubmitError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [enrolledCourseIds, setEnrolledCourseIds] = useState(() => new Set());

    const query = useMemo(() => ({ page, size: PAGE_SIZE }), [page]);

    const loadCourses = useCallback(async () => {
        setIsLoading(true);
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
            toast.error("Failed to load courses", mapApiError(loadError));
        } finally {
            setIsLoading(false);
        }
    }, [role, query, toast]);

    const syncStudentEnrollments = useCallback(async () => {
        if (role !== "STUDENT") return;
        try {
            const enrollmentPage = await fetchMyEnrollments({ page: 0, size: ENROLLMENT_SYNC_PAGE_SIZE });
            const next = new Set((enrollmentPage?.content ?? []).map((item) => item.courseId));
            setEnrolledCourseIds(next);
        } catch (syncError) {
            toast.error("Sync failed", mapEnrollmentError(syncError));
        }
    }, [role, toast]);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect -- Data fetching on mount is a standard pattern in plain React.
        loadCourses();
    }, [loadCourses]);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect -- Data fetching on mount is a standard pattern in plain React.
        syncStudentEnrollments();
    }, [syncStudentEnrollments]);

    async function handleCreateCourse(payload) {
        setIsSubmitting(true);
        setSubmitError("");
        try {
            await createCourse(payload);
            setEditor(null);
            toast.success("Success", "Course created successfully.");
            await loadCourses();
        } catch (submitErr) {
            setSubmitError(mapApiError(submitErr));
        } finally {
            setIsSubmitting(false);
        }
    }

    async function handleUpdateCourse(payload) {
        if (!editor?.course?.id) return;
        setIsSubmitting(true);
        setSubmitError("");
        try {
            await updateCourse(editor.course.id, payload);
            setEditor(null);
            toast.success("Success", "Course updated successfully.");
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
        if (!confirmed) return;

        try {
            const updatedCourse = course.isActive
                ? await deactivateCourse(course.id).then(() => ({ ...course, isActive: false }))
                : await activateCourse(course.id).then(() => ({ ...course, isActive: true }));
            setCoursesPage((prev) => {
                if (!prev) return prev;
                return {
                    ...prev,
                    content: (prev.content ?? []).map((item) => (item.id === course.id ? { ...item, ...updatedCourse } : item)),
                };
            });
            toast.success("Status Updated", `Course ${updatedCourse.isActive ? "activated" : "deactivated"}.`);
        } catch (actionErr) {
            toast.error("Action Failed", mapApiError(actionErr));
        }
    }

    function handleEnrollSuccess(course) {
        toast.success("Enrolled", `Successfully enrolled in "${course.title}".`);
        setEnrolledCourseIds((prev) => {
            const next = new Set(prev);
            next.add(course.id);
            return next;
        });

        setCoursesPage((prev) => {
            if (!prev) return prev;
            return {
                ...prev,
                content: (prev.content ?? []).map((item) => {
                    if (item.id !== course.id) return item;
                    return {
                        ...item,
                        availableSeats: Math.max((item.availableSeats ?? 0) - 1, 0),
                    };
                }),
            };
        });
    }

    function handleEnrollError(message) {
        toast.error("Enrollment failed", message);
    }

    const handleOpenEnrollments = useCallback((course) => {
        if (!course?.id) return;
        onNavigate?.(`/enrollments?courseId=${encodeURIComponent(String(course.id))}`);
    }, [onNavigate]);

    const courses = coursesPage?.content ?? [];

    return (
        <div className="min-h-screen bg-zinc-950 text-zinc-100">
            <main className="mx-auto w-full max-w-6xl space-y-8 px-4 py-8">
                <PageHeader
                    eyebrow={role === "INSTRUCTOR" ? "Instructor workspace" : role === "ADMIN" ? "Administration" : "Student workspace"}
                    title="Courses"
                    subtitle={role === "INSTRUCTOR"
                        ? "Create, edit, activate, and deactivate the courses you own."
                        : role === "ADMIN"
                            ? "Review the full course catalog across the platform."
                            : "Browse active courses and enroll in the ones that fit your goals."}
                    actions={(
                        <div className="flex gap-2">
                            {role === "INSTRUCTOR" ? (
                                <Button
                                    type="button"
                                    variant="primary"
                                    size="sm"
                                    onClick={() => {
                                        setEditor({ mode: "create", course: null });
                                        setSubmitError("");
                                    }}
                                >
                                    <Plus className="mr-1 h-4 w-4" />
                                    Create Course
                                </Button>
                            ) : null}
                            <Button type="button" variant="outline" size="sm" onClick={() => onNavigate?.("/dashboard")}>Dashboard</Button>
                        </div>
                    )}
                />

                <Modal 
                    isOpen={!!editor} 
                    onClose={() => { setEditor(null); setSubmitError(""); }}
                    title={editor?.mode === "edit" ? "Edit Course" : "Create Course"}
                    description={editor?.mode === "edit" ? "Update the course details below." : "Fill in the details to create a new course."}
                >
                    {editor ? (
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
                    ) : null}
                </Modal>

                {isLoading ? (
                    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                        {Array.from({ length: 6 }).map((_, i) => (
                            <Skeleton key={i} className="h-64 w-full rounded-2xl" />
                        ))}
                    </div>
                ) : (
                    <CourseList
                        courses={courses}
                        role={role}
                        onEdit={(course) => {
                            setEditor({ mode: "edit", course });
                            setSubmitError("");
                        }}
                        onDeactivate={handleToggleCourseStatus}
                        onOpenEnrollments={handleOpenEnrollments}
                        onEnroll={handleEnrollSuccess}
                        onEnrollError={handleEnrollError}
                        enrolledCourseIds={enrolledCourseIds}
                        emptyState={role === "INSTRUCTOR"
                            ? {
                                title: "No courses yet",
                                description: "Create your first course to start managing enrollments and course content.",
                                actionLabel: "Create Course",
                                actionVariant: "primary",
                                onAction: () => {
                                    setEditor({ mode: "create", course: null });
                                    setSubmitError("");
                                },
                            }
                            : role === "STUDENT"
                                ? {
                                    title: "No active courses right now",
                                    description: "There are no active courses available for enrollment at the moment.",
                                    actionLabel: "Dashboard",
                                    actionVariant: "outline",
                                    onAction: () => onNavigate?.("/dashboard"),
                                }
                                : {
                                    title: "No courses found",
                                    description: "There are no courses to display for this view yet.",
                                    actionLabel: "Dashboard",
                                    actionVariant: "outline",
                                    onAction: () => onNavigate?.("/dashboard"),
                                }}
                    />
                )}

                {!isLoading && coursesPage && coursesPage.totalPages > 1 ? (
                    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-zinc-800/80 bg-zinc-900/50 px-4 py-3 text-sm text-zinc-300">
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
