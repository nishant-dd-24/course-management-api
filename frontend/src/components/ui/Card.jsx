import { createElement } from "react";
import { cn } from "../../lib/cn";

export default function Card({ as: Wrapper = "div", className, children, ...props }) {
    return createElement(
        Wrapper,
        {
            className: cn(
                "rounded-2xl border border-zinc-800/60 bg-zinc-900/40 p-6 shadow-sm backdrop-blur-md",
                className
            ),
            ...props,
        },
        children
    );
}

