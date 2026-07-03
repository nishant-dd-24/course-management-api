import { ApiError, apiFetch } from "./client";

function toQuery(params = {}) {
    const query = new URLSearchParams();

    Object.entries(params).forEach(([key, value]) => {
        if (value === undefined || value === null || value === "") {
            return;
        }

        query.set(key, String(value));
    });

    const encoded = query.toString();
    return encoded ? `?${encoded}` : "";
}

export function enrollInCourse(courseId) {
    return apiFetch(`/enrollments/${courseId}`, {
        method: "POST",
    });
}

export function unenrollFromCourse(courseId) {
    return apiFetch(`/enrollments/${courseId}`, {
        method: "DELETE",
    });
}

export function fetchMyEnrollments(params = {}) {
    return apiFetch(`/enrollments/my${toQuery(params)}`);
}

export function fetchCourseEnrollments(courseId, params = {}) {
    return apiFetch(`/enrollments/${courseId}${toQuery(params)}`);
}

export function mapEnrollmentError(error) {
    if (error instanceof ApiError && error.status === 403) {
        return "You are not allowed to perform this action.";
    }

    if (error instanceof ApiError && error.status === 404) {
        return "Enrollment or course not found.";
    }

    if (error instanceof ApiError && error.status === 409) {
        return error.message || "Already enrolled or course seats are full.";
    }

    if (error instanceof ApiError && error.status === 400) {
        return error.message || "Invalid request. Please check your input.";
    }

    return "Unable to process enrollment right now.";
}

