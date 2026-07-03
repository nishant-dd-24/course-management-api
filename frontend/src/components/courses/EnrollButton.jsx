import { useMemo, useState } from "react";
import { enrollInCourse, mapEnrollmentError } from "../../api/enrollments";
import Button from "../ui/Button";

export default function EnrollButton({ courseId, isEnrolled, hasSeats, onEnrolled, onError }) {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState("");

    const disabledReason = useMemo(() => {
        if (isSubmitting) {
            return "submitting";
        }

        if (isEnrolled) {
            return "enrolled";
        }

        if (!hasSeats) {
            return "full";
        }

        return "";
    }, [isSubmitting, isEnrolled, hasSeats]);

    async function handleEnroll() {
        setError("");
        setIsSubmitting(true);

        try {
            const enrollment = await enrollInCourse(courseId);
            onEnrolled?.(enrollment);
        } catch (actionError) {
            const message = mapEnrollmentError(actionError);
            setError(message);
            onError?.(message);
        } finally {
            setIsSubmitting(false);
        }
    }

    const label = disabledReason === "enrolled"
        ? "Enrolled"
        : disabledReason === "full"
            ? "Course Full"
            : isSubmitting
                ? "Enrolling..."
                : "Enroll";

    return (
        <div>
            <Button
                type="button"
                onClick={handleEnroll}
                disabled={Boolean(disabledReason)}
                variant="primary"
                size="sm"
            >
                {label}
            </Button>
            {error ? <p className="mt-2 text-xs text-red-300">{error}</p> : null}
        </div>
    );
}

