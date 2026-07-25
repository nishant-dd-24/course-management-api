import { cn } from "../../lib/cn";

const variants = {
    neutral: "border-zinc-500/20 bg-zinc-500/10 text-zinc-300",
    active: "border-emerald-500/20 bg-emerald-500/10 text-emerald-400",
    inactive: "border-red-500/20 bg-red-500/10 text-red-400",
    success: "border-emerald-500/20 bg-emerald-500/10 text-emerald-400",
    danger: "border-red-500/20 bg-red-500/10 text-red-400",
    warning: "border-amber-500/20 bg-amber-500/10 text-amber-400",
    primary: "border-blue-500/20 bg-blue-500/10 text-blue-400",
    admin: "border-red-500/20 bg-red-500/10 text-red-400",
    instructor: "border-blue-500/20 bg-blue-500/10 text-blue-400",
    student: "border-zinc-500/20 bg-zinc-500/10 text-zinc-300",
};

const dotColors = {
    active: "bg-emerald-500",
    inactive: "bg-red-500",
    success: "bg-emerald-500",
    danger: "bg-red-500",
    warning: "bg-amber-500",
    primary: "bg-blue-500",
    admin: "bg-red-500",
    instructor: "bg-blue-500",
};

export default function Badge({ variant = "neutral", className, children, ...props }) {
    const dotColor = dotColors[variant];

    return (
        <span
            className={cn(
                "inline-flex items-center gap-1.5 rounded-md border px-2 py-0.5 text-xs font-medium backdrop-blur-sm",
                variants[variant] ?? variants.neutral,
                className
            )}
            {...props}
        >
            {dotColor && <span className={cn("h-1.5 w-1.5 rounded-full", dotColor)} />}
            {children}
        </span>
    );
}
