import { useMemo, useState } from "react";
import Button from "../ui/Button";

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

    const canSubmit = useMemo(() => {
        if (isSubmitting) {
            return false;
        }

        return values.title.trim().length > 0 && values.description.trim().length > 0;
    }, [values, isSubmitting]);

    function setField(field, value) {
        setValues((prev) => ({ ...prev, [field]: value }));
        setFieldErrors((prev) => {
            if (!prev[field]) return prev;
            const next = { ...prev };
            delete next[field];
            return next;
        });
    }

    function validate() {
        const nextErrors = {};
        if (!values.title.trim()) nextErrors.title = "Title is required.";
        if (!values.description.trim()) nextErrors.description = "Description is required.";
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
        if (!validate()) return;
        onSubmit({
            title: values.title.trim(),
            description: values.description.trim(),
            maxSeats: values.maxSeats ? Number(values.maxSeats) : null,
        });
    }

    return (
        <form className="space-y-5" onSubmit={handleSubmit}>
            {submitError ? (
                <p className="rounded-lg border border-red-500/20 bg-red-500/10 px-3 py-2 text-sm text-red-400">
                    {submitError}
                </p>
            ) : null}

            <label className="block text-sm">
                <span className="mb-1.5 block font-medium text-zinc-300">Title</span>
                <input
                    className="w-full rounded-lg border border-zinc-700 bg-zinc-950/50 px-3 py-2 text-zinc-100 outline-none transition focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50"
                    value={values.title}
                    onChange={(event) => setField("title", event.target.value)}
                    placeholder="Java Backend Mastery"
                    required
                />
                {fieldErrors.title ? <span className="mt-1.5 block text-xs text-red-400">{fieldErrors.title}</span> : null}
            </label>

            <label className="block text-sm">
                <span className="mb-1.5 block font-medium text-zinc-300">Description</span>
                <textarea
                    className="min-h-[100px] w-full rounded-lg border border-zinc-700 bg-zinc-950/50 px-3 py-2 text-zinc-100 outline-none transition focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50"
                    value={values.description}
                    onChange={(event) => setField("description", event.target.value)}
                    rows={3}
                    placeholder="Build production-grade APIs with Spring Boot"
                    required
                />
                {fieldErrors.description ? <span className="mt-1.5 block text-xs text-red-400">{fieldErrors.description}</span> : null}
            </label>

            <label className="block text-sm">
                <span className="mb-1.5 block font-medium text-zinc-300">Max seats (optional)</span>
                <input
                    className="w-full rounded-lg border border-zinc-700 bg-zinc-950/50 px-3 py-2 text-zinc-100 outline-none transition focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50"
                    value={values.maxSeats}
                    onChange={(event) => setField("maxSeats", event.target.value)}
                    type="number"
                    min="1"
                    placeholder="Unlimited"
                />
                {fieldErrors.maxSeats ? <span className="mt-1.5 block text-xs text-red-400">{fieldErrors.maxSeats}</span> : null}
            </label>

            <div className="flex justify-end gap-2 pt-2">
                <Button type="button" onClick={onCancel} variant="ghost">
                    Cancel
                </Button>
                <Button type="submit" disabled={!canSubmit} variant="primary" isLoading={isSubmitting}>
                    {mode === "edit" ? "Save changes" : "Create course"}
                </Button>
            </div>
        </form>
    );
}
