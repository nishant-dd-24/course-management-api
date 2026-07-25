import { useMemo, useState } from "react";
import { enrollInCourse, mapEnrollmentError } from "../../api/enrollments";
import Button from "../ui/Button";

export default function EnrollButton({ courseId, isEnrolled, hasSeats, onEnrolled, onError }) {
    const [isSubmitting, setIsSubmitting] = useState(false);

    const disabledReason = useMemo(() => {
        if (isSubmitting) return "submitting";
        if (isEnrolled) return "enrolled";
        if (!hasSeats) return "full";
        return "";
    }, [isSubmitting, isEnrolled, hasSeats]);

    async function handleEnroll() {
        setIsSubmitting(true);
        try {
            const enrollment = await enrollInCourse(courseId);
            onEnrolled?.(enrollment);
        } catch (actionError) {
            const message = mapEnrollmentError(actionError);
            onError?.(message);
        } finally {
            setIsSubmitting(false);
        }
    }

    const label = disabledReason === "enrolled"
        ? "Enrolled"
        : disabledReason === "full"
            ? "Course Full"
            : "Enroll";

    return (
        <Button
            type="button"
            onClick={handleEnroll}
            disabled={Boolean(disabledReason)}
            variant={disabledReason === "enrolled" ? "success" : "primary"}
            size="sm"
            isLoading={isSubmitting}
        >
            {label}
        </Button>
    );
}
