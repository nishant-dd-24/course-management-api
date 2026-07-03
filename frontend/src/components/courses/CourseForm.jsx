import { useMemo, useState } from "react";
import Button from "../ui/Button";
import Card from "../ui/Card";

function normalizeInitialValues(initialValues) {
    return {
        title: initialValues?.title ?? "",
        description: initialValues?.description ?? "",
        maxSeats: initialValues?.maxSeats ? String(initialValues.maxSeats) : "",
    };
}

export default function CourseForm({
    mode,
    initialValues,
    isSubmitting,
    submitError,
    onSubmit,
    onCancel,
}) {
    const [values, setValues] = useState(() => normalizeInitialValues(initialValues));
    const [fieldErrors, setFieldErrors] = useState({});

    const title = mode === "edit" ? "Edit course" : "Create course";

    const canSubmit = useMemo(() => {
        if (isSubmitting) {
            return false;
        }

        return values.title.trim().length > 0 && values.description.trim().length > 0;
    }, [values, isSubmitting]);

    function setField(field, value) {
        setValues((prev) => ({ ...prev, [field]: value }));
        setFieldErrors((prev) => {
            if (!prev[field]) {
                return prev;
            }

            const next = { ...prev };
            delete next[field];
            return next;
        });
    }

    function validate() {
        const nextErrors = {};

        if (!values.title.trim()) {
            nextErrors.title = "Title is required.";
        }

        if (!values.description.trim()) {
            nextErrors.description = "Description is required.";
        }

        if (values.maxSeats) {
            const numericSeats = Number(values.maxSeats);

            if (!Number.isFinite(numericSeats) || numericSeats < 1) {
                nextErrors.maxSeats = "Max seats must be at least 1.";
            }
        }

        setFieldErrors(nextErrors);
        return Object.keys(nextErrors).length === 0;
    }

    function handleSubmit(event) {
        event.preventDefault();

        if (!validate()) {
            return;
        }

        onSubmit({
            title: values.title.trim(),
            description: values.description.trim(),
            maxSeats: values.maxSeats ? Number(values.maxSeats) : null,
        });
    }

    return (
        <Card as="form" className="space-y-5" onSubmit={handleSubmit}>
            <div className="space-y-1">
                <h3 className="text-xl font-semibold tracking-tight text-slate-50">{title}</h3>
                <p className="text-sm leading-6 text-slate-300">Provide the course details below. You can update the course later if needed.</p>
            </div>

            {submitError ? (
                <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
                    {submitError}
                </p>
            ) : null}

            <label className="block text-sm">
                <span className="mb-1.5 block font-medium text-slate-300">Title</span>
                <input
                    className="w-full rounded-xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-slate-100 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                    value={values.title}
                    onChange={(event) => setField("title", event.target.value)}
                    placeholder="Java Backend Mastery"
                    required
                />
                {fieldErrors.title ? <span className="mt-1.5 block text-xs text-red-300">{fieldErrors.title}</span> : null}
            </label>

            <label className="block text-sm">
                <span className="mb-1.5 block font-medium text-slate-300">Description</span>
                <textarea
                    className="min-h-28 w-full rounded-xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-slate-100 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                    value={values.description}
                    onChange={(event) => setField("description", event.target.value)}
                    rows={3}
                    placeholder="Build production-grade APIs with Spring Boot"
                    required
                />
                {fieldErrors.description ? <span className="mt-1.5 block text-xs text-red-300">{fieldErrors.description}</span> : null}
            </label>

            <label className="block text-sm">
                <span className="mb-1.5 block font-medium text-slate-300">Max seats (optional)</span>
                <input
                    className="w-full rounded-xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-slate-100 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                    value={values.maxSeats}
                    onChange={(event) => setField("maxSeats", event.target.value)}
                    type="number"
                    min="1"
                    placeholder="20"
                />
                {fieldErrors.maxSeats ? <span className="mt-1.5 block text-xs text-red-300">{fieldErrors.maxSeats}</span> : null}
            </label>

            <div className="flex flex-wrap gap-2">
                <Button
                    type="submit"
                    disabled={!canSubmit}
                    variant="primary"
                >
                    {isSubmitting ? (mode === "edit" ? "Saving..." : "Creating...") : (mode === "edit" ? "Save" : "Create")}
                </Button>
                <Button
                    type="button"
                    onClick={onCancel}
                    variant="outline"
                >
                    Cancel
                </Button>
            </div>
        </Card>
    );
}

