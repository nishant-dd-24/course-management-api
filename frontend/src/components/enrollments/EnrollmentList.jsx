import Badge from "../ui/Badge";
import Button from "../ui/Button";
import Card from "../ui/Card";
import EmptyState from "../ui/EmptyState";

export default function EnrollmentList({
    enrollments,
    mode,
    onUnenroll,
    isActionLoading,
    emptyState,
}) {
    if (!enrollments || enrollments.length === 0) {
        return emptyState ? <EmptyState {...emptyState} /> : null;
    }

    return (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {enrollments.map((enrollment) => (
                <Card key={enrollment.id} className="space-y-4">
                    <div className="flex items-start justify-between gap-3">
                        <div>
                            <h3 className="text-lg font-semibold tracking-tight text-slate-50">Enrollment #{enrollment.id}</h3>
                            <p className="mt-2 text-sm leading-6 text-slate-300">Course #{enrollment.courseId}</p>
                        </div>
                        <Badge variant={mode === "student" ? "primary" : "neutral"}>
                            {mode === "student" ? "My Enrollment" : "Course Roster"}
                        </Badge>
                    </div>

                    <div className="grid gap-3 sm:grid-cols-2">
                        <div className="rounded-2xl border border-slate-800/80 bg-slate-950/50 px-4 py-3">
                            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Course ID</p>
                            <p className="mt-1 text-sm font-medium text-slate-100">{enrollment.courseId}</p>
                        </div>
                        <div className="rounded-2xl border border-slate-800/80 bg-slate-950/50 px-4 py-3">
                            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Student ID</p>
                            <p className="mt-1 text-sm font-medium text-slate-100">{enrollment.studentId}</p>
                        </div>
                    </div>

                    {mode === "student" ? (
                        <Button
                            type="button"
                            onClick={() => onUnenroll?.(enrollment)}
                            disabled={isActionLoading}
                            variant="danger"
                            size="sm"
                        >
                            {isActionLoading ? "Processing..." : "Unenroll"}
                        </Button>
                    ) : null}
                </Card>
            ))}
        </div>
    );
}

