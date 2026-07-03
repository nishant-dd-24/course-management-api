import CourseCard from "./CourseCard";
import EmptyState from "../ui/EmptyState";

export default function CourseList({
    courses,
    role,
    onEdit,
    onDeactivate,
    onOpenEnrollments,
    onEnroll,
    onEnrollError,
    enrolledCourseIds,
    emptyState,
}) {
    if (!courses || courses.length === 0) {
        return emptyState ? <EmptyState {...emptyState} /> : null;
    }

    return (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {courses.map((course) => (
                <CourseCard
                    key={course.id}
                    course={course}
                    role={role}
                    onEdit={onEdit}
                    onDeactivate={onDeactivate}
                    onOpenEnrollments={onOpenEnrollments}
                    onEnroll={onEnroll}
                    onEnrollError={onEnrollError}
                    isEnrolled={enrolledCourseIds?.has(course.id) ?? false}
                />
            ))}
        </div>
    );
}
