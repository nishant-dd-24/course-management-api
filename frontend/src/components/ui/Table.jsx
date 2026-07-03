import { cn } from "../../lib/cn";

export default function Table({ className, children }) {
    return (
        <div data-ui="table" className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/95 shadow-[0_1px_0_rgba(255,255,255,0.02)]">
            <div className="overflow-x-auto">
                <table className={cn("min-w-full border-separate border-spacing-0 text-left text-sm text-slate-200", className)}>
                    {children}
                </table>
            </div>
        </div>
    );
}

