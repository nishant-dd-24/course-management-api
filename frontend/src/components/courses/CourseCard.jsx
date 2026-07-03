import EnrollButton from "./EnrollButton";
import Badge from "../ui/Badge";
import Button from "../ui/Button";
import Card from "../ui/Card";

export default function CourseCard({
    course,
    role,
    onEdit,
    onDeactivate,
    onOpenEnrollments,
    onEnroll,
    onEnrollError,
    isEnrolled,
}) {
    return (
        <Card className="flex h-full flex-col gap-5">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-2">
                    <h3 className="text-xl font-semibold tracking-tight text-slate-50">{course.title}</h3>
                    <p className="max-w-3xl text-sm leading-6 text-slate-300">{course.description}</p>
                </div>

                <div className="flex flex-col items-end gap-2">
                    <Badge variant={course.isActive ? "active" : "inactive"}>
                        {course.isActive ? "Active" : "Inactive"}
                    </Badge>
                    {role === "INSTRUCTOR" ? <Badge variant="instructor">Instructor</Badge> : null}
                    {role === "ADMIN" ? <Badge variant="admin">Admin</Badge> : null}
                </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
                <div className="rounded-2xl border border-slate-800/80 bg-slate-950/50 px-4 py-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Instructor</p>
                    <p className="mt-1 text-sm font-medium text-slate-100">ID {course.instructorId}</p>
                </div>
                <div className="rounded-2xl border border-slate-800/80 bg-slate-950/50 px-4 py-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Available seats</p>
                    <p className="mt-1 text-sm font-medium text-slate-100">
                        {course.availableSeats} / {course.maxSeats}
                    </p>
                </div>
            </div>

            {role === "STUDENT" ? (
                <div className="flex justify-end">
                    <EnrollButton
                        courseId={course.id}
                        isEnrolled={isEnrolled}
                        hasSeats={Number(course.availableSeats ?? 0) > 0}
                        onEnrolled={() => onEnroll?.(course)}
                        onError={onEnrollError}
                    />
                </div>
            ) : null}

            {role === "INSTRUCTOR" ? (
                <div className="flex flex-wrap justify-end gap-2">
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => onOpenEnrollments?.(course)}
                    >
                        Open Enrollments
                    </Button>
                    <Button type="button" variant="warning" size="sm" onClick={() => onEdit?.(course)}>
                        Edit
                    </Button>
                    <Button
                        type="button"
                        variant={course.isActive ? "danger" : "success"}
                        size="sm"
                        onClick={() => onDeactivate?.(course)}
                    >
                        {course.isActive ? "Deactivate" : "Activate"}
                    </Button>
                </div>
            ) : null}
        </Card>
    );
}

