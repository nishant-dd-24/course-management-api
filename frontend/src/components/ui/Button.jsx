import { cn } from "../../lib/cn";
import { Loader2 } from "lucide-react";

const variants = {
    primary: "bg-blue-600 text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.2)] hover:bg-blue-500 focus-visible:ring-blue-500",
    success: "border border-emerald-500/30 bg-emerald-500/10 text-emerald-300 hover:border-emerald-500/50 hover:bg-emerald-500/20 focus-visible:ring-emerald-500",
    warning: "border border-amber-500/30 bg-amber-500/10 text-amber-300 hover:border-amber-500/50 hover:bg-amber-500/20 focus-visible:ring-amber-500",
    danger: "bg-red-600 text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.2)] hover:bg-red-500 focus-visible:ring-red-500",
    secondary: "border border-zinc-700 bg-zinc-800 text-zinc-100 hover:border-zinc-600 hover:bg-zinc-700 focus-visible:ring-zinc-400",
    outline: "border border-zinc-700 bg-transparent text-zinc-200 hover:border-zinc-600 hover:bg-zinc-800 focus-visible:ring-zinc-400",
    ghost: "bg-transparent text-zinc-300 hover:bg-zinc-800/70 hover:text-zinc-50 focus-visible:ring-zinc-400",
};

const sizes = {
    sm: "h-8 px-3 text-xs",
    md: "h-10 px-4 text-sm",
    lg: "h-11 px-5 text-base",
};

export default function Button({
    variant = "secondary",
    size = "md",
    className,
    type = "button",
    isLoading = false,
    children,
    disabled,
    ...props
}) {
    return (
        <button
            type={type}
            disabled={disabled || isLoading}
            className={cn(
                "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg font-medium transition-all duration-200 ease-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950 disabled:pointer-events-none disabled:opacity-50",
                variants[variant] ?? variants.secondary,
                sizes[size] ?? sizes.md,
                className
            )}
            data-variant={variant}
            {...props}
        >
            {isLoading && <Loader2 className="h-4 w-4 animate-spin" />}
            {children}
        </button>
    );
}
