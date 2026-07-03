import { cn } from "../../lib/cn";

const variants = {
    neutral: "border-slate-700 bg-slate-800 text-slate-200",
    active: "border-emerald-500/30 bg-emerald-500/15 text-emerald-200",
    inactive: "border-red-500/30 bg-red-500/15 text-red-200",
    success: "border-emerald-500/30 bg-emerald-500/15 text-emerald-200",
    danger: "border-red-500/30 bg-red-500/15 text-red-200",
    warning: "border-amber-500/30 bg-amber-500/15 text-amber-200",
    primary: "border-blue-500/30 bg-blue-500/15 text-blue-200",
    admin: "border-red-500/30 bg-red-500/15 text-red-200",
    instructor: "border-blue-500/30 bg-blue-500/15 text-blue-200",
    student: "border-slate-500/30 bg-slate-500/15 text-slate-200",
};

export default function Badge({ variant = "neutral", className, children, ...props }) {
    return (
        <span
            className={cn(
                "inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.2em] leading-none",
                variants[variant] ?? variants.neutral,
                className
            )}
            {...props}
        >
            {children}
        </span>
    );
}

