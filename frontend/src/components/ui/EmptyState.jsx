import Button from "./Button";
import Card from "./Card";
import { Info } from "lucide-react";

export default function EmptyState({
    icon = Info,
    title,
    description,
    actionLabel,
    onAction,
    actionVariant = "primary",
}) {
    const Icon = icon;
    return (
        <Card className="flex flex-col items-center justify-center border-dashed border-zinc-800/80 bg-zinc-900/30 px-6 py-14 text-center">
            <div className="mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-zinc-800/50 text-zinc-400 ring-1 ring-zinc-700/50">
                <Icon className="h-7 w-7" />
            </div>
            <h3 className="text-lg font-semibold tracking-tight text-zinc-100">{title}</h3>
            <p className="mt-2 max-w-sm text-sm leading-6 text-zinc-400">{description}</p>
            {actionLabel && onAction ? (
                <div className="mt-6">
                    <Button variant={actionVariant} onClick={onAction}>
                        {actionLabel}
                    </Button>
                </div>
            ) : null}
        </Card>
    );
}
