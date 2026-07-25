import Badge from "../ui/Badge";
import Button from "../ui/Button";
import Card from "../ui/Card";
import EmptyState from "../ui/EmptyState";
import { BookMarked } from "lucide-react";

export default function EnrollmentList({
    enrollments,
    mode,
    onUnenroll,
    isActionLoading,
    emptyState,
}) {
    if (!enrollments || enrollments.length === 0) {
        return emptyState ? <EmptyState {...emptyState} icon={BookMarked} /> : null;
    }

    return (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {enrollments.map((enrollment) => (
                <Card key={enrollment.id} className="flex flex-col h-full gap-4">
                    <div className="flex items-start justify-between gap-3">
                        <div>
                            <h3 className="text-lg font-semibold tracking-tight text-zinc-100">Enrollment #{enrollment.id}</h3>
                            <p className="mt-1 text-sm leading-6 text-zinc-400">Course #{enrollment.courseId}</p>
                        </div>
                        <Badge variant={mode === "student" ? "primary" : "neutral"}>
                            {mode === "student" ? "My Enrollment" : "Course Roster"}
                        </Badge>
                    </div>

                    <div className="grid gap-3 sm:grid-cols-2 mt-auto">
                        <div className="rounded-lg border border-zinc-800/40 bg-zinc-950/50 px-3 py-2.5">
                            <p className="text-xs font-medium text-zinc-500 mb-1">Course ID</p>
                            <p className="text-sm font-medium text-zinc-300">{enrollment.courseId}</p>
                        </div>
                        <div className="rounded-lg border border-zinc-800/40 bg-zinc-950/50 px-3 py-2.5">
                            <p className="text-xs font-medium text-zinc-500 mb-1">Student ID</p>
                            <p className="text-sm font-medium text-zinc-300">{enrollment.studentId}</p>
                        </div>
                    </div>

                    {mode === "student" ? (
                        <div className="pt-2 border-t border-zinc-800/50 flex justify-end">
                            <Button
                                type="button"
                                onClick={() => onUnenroll?.(enrollment)}
                                disabled={isActionLoading}
                                isLoading={isActionLoading}
                                variant="danger"
                                size="sm"
                            >
                                Unenroll
                            </Button>
                        </div>
                    ) : null}
                </Card>
            ))}
        </div>
    );
}
