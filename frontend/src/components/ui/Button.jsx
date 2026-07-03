import { cn } from "../../lib/cn";

const variants = {
    primary: "bg-blue-500 text-white shadow-sm shadow-blue-500/20 hover:bg-blue-400 focus-visible:ring-blue-500/30",
    success: "border border-emerald-500/30 bg-emerald-500/15 text-emerald-200 hover:border-emerald-400/40 hover:bg-emerald-500/20 focus-visible:ring-emerald-500/30",
    warning: "border border-amber-500/30 bg-amber-500/15 text-amber-200 hover:border-amber-400/40 hover:bg-amber-500/20 focus-visible:ring-amber-500/30",
    danger: "border border-red-500/30 bg-red-500/15 text-red-200 hover:border-red-400/40 hover:bg-red-500/20 focus-visible:ring-red-500/30",
    secondary: "border border-slate-700 bg-slate-800/80 text-slate-100 hover:border-slate-600 hover:bg-slate-700/80 focus-visible:ring-slate-500/30",
    outline: "border border-slate-700 bg-transparent text-slate-200 hover:border-slate-500 hover:bg-slate-800/70 focus-visible:ring-slate-500/30",
    ghost: "bg-transparent text-slate-300 hover:bg-slate-800/70 hover:text-slate-50 focus-visible:ring-slate-500/30",
};

const sizes = {
    sm: "h-9 px-3 text-sm",
    md: "h-11 px-4 text-sm",
    lg: "h-12 px-5 text-sm",
};

export default function Button({
    variant = "secondary",
    size = "md",
    className,
    type = "button",
    ...props
}) {
    return (
        <button
            type={type}
            className={cn(
                "inline-flex min-h-11 items-center justify-center gap-2 whitespace-nowrap rounded-xl font-medium tracking-[0.01em] transition duration-150 ease-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-0 disabled:pointer-events-none disabled:opacity-50",
                variants[variant] ?? variants.secondary,
                sizes[size] ?? sizes.md,
                className
            )}
            data-variant={variant}
            {...props}
        />
    );
}

