import { createElement } from "react";
import { cn } from "../../lib/cn";

export default function Card({ as: Wrapper = "div", className, children, ...props }) {
    return createElement(
        Wrapper,
        {
            className: cn(
                "rounded-2xl border border-slate-800/80 bg-slate-900/95 p-6 shadow-[0_1px_0_rgba(255,255,255,0.02)] transition duration-150 ease-out hover:border-slate-700 hover:shadow-lg hover:shadow-slate-950/40",
                className
            ),
            ...props,
        },
        children
    );
}

