import { apiFetch } from "./client";

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

export function fetchActiveCourses(params = {}) {
    return apiFetch(`/courses/active${toQuery(params)}`);
}

export function fetchMyCourses(params = {}) {
    return apiFetch(`/courses/my${toQuery(params)}`);
}

export function fetchAllCourses(params = {}) {
    return apiFetch(`/courses${toQuery(params)}`);
}

export function createCourse(payload) {
    return apiFetch("/courses", {
        method: "POST",
        body: JSON.stringify(payload),
    });
}

export function updateCourse(courseId, payload) {
    return apiFetch(`/courses/${courseId}`, {
        method: "PUT",
        body: JSON.stringify(payload),
    });
}

export function deactivateCourse(courseId) {
    return apiFetch(`/courses/${courseId}`, {
        method: "DELETE",
    });
}

export function activateCourse(courseId) {
    return apiFetch(`/courses/${courseId}/activate`, {
        method: "POST",
    });
}

