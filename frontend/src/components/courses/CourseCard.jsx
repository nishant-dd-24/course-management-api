import EnrollButton from "./EnrollButton";
import Badge from "../ui/Badge";
import Button from "../ui/Button";
import Card from "../ui/Card";
import { Users, Edit2, Play, Square, Users2 } from "lucide-react";

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
        <Card className="flex h-full flex-col p-0 overflow-hidden group">
            {/* Banner placeholder */}
            <div className="h-24 w-full bg-gradient-to-br from-zinc-800 to-zinc-900 border-b border-zinc-800/80 relative">
                <div className="absolute top-3 right-3 flex gap-2">
                    <Badge variant={course.isActive ? "active" : "inactive"}>
                        {course.isActive ? "Active" : "Inactive"}
                    </Badge>
                </div>
            </div>

            <div className="flex flex-col flex-1 p-5 pt-4">
                <div className="mb-4">
                    <h3 className="text-base font-semibold text-zinc-100 line-clamp-1 group-hover:text-zinc-50 transition-colors">{course.title}</h3>
                    <p className="mt-1 text-sm leading-6 text-zinc-400 line-clamp-2">{course.description}</p>
                </div>

                <div className="mt-auto grid grid-cols-2 gap-3 pb-5">
                    <div className="rounded-lg bg-zinc-950/50 px-3 py-2.5 border border-zinc-800/40">
                        <div className="flex items-center text-xs font-medium text-zinc-500 mb-1">
                            <Users2 className="w-3.5 h-3.5 mr-1.5" /> Instructor
                        </div>
                        <p className="text-sm font-medium text-zinc-300">ID {course.instructorId}</p>
                    </div>
                    <div className="rounded-lg bg-zinc-950/50 px-3 py-2.5 border border-zinc-800/40">
                        <div className="flex items-center text-xs font-medium text-zinc-500 mb-1">
                            <Users className="w-3.5 h-3.5 mr-1.5" /> Seats
                        </div>
                        <p className="text-sm font-medium text-zinc-300">
                            {course.availableSeats} <span className="text-zinc-600">/ {course.maxSeats}</span>
                        </p>
                    </div>
                </div>

                {role === "STUDENT" ? (
                    <div className="pt-2 border-t border-zinc-800/50 flex justify-end">
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
                    <div className="pt-3 border-t border-zinc-800/50 flex flex-wrap justify-end gap-2">
                        <Button type="button" variant="ghost" size="sm" onClick={() => onOpenEnrollments?.(course)} title="Open Enrollments">
                            <Users className="h-4 w-4" />
                        </Button>
                        <Button type="button" variant="ghost" size="sm" onClick={() => onEdit?.(course)} title="Edit">
                            <Edit2 className="h-4 w-4" />
                        </Button>
                        <Button
                            type="button"
                            variant="ghost"
                            className={course.isActive ? "text-amber-500 hover:text-amber-400" : "text-emerald-500 hover:text-emerald-400"}
                            size="sm"
                            onClick={() => onDeactivate?.(course)}
                            title={course.isActive ? "Deactivate" : "Activate"}
                        >
                            {course.isActive ? <Square className="h-4 w-4" /> : <Play className="h-4 w-4" />}
                        </Button>
                    </div>
                ) : null}
            </div>
        </Card>
    );
}
