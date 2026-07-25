import { cn } from "../../lib/cn";

export default function Table({ className, children }) {
    return (
        <div data-ui="table" className="overflow-hidden rounded-xl border border-zinc-800/80 bg-zinc-900/50 shadow-sm backdrop-blur-sm">
            <div className="overflow-x-auto">
                <table className={cn("min-w-full border-separate border-spacing-0 text-left text-sm text-zinc-300", className)}>
                    {children}
                </table>
            </div>
        </div>
    );
}

