import Button from "./Button";
import Card from "./Card";

export default function EmptyState({
    icon = "ℹ️",
    title,
    description,
    actionLabel,
    onAction,
    actionVariant = "primary",
}) {
    return (
        <Card className="border-dashed border-slate-700/80 px-6 py-10 text-center">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-800 text-3xl shadow-inner shadow-slate-950/30">
                {icon}
            </div>
            <h3 className="mt-5 text-xl font-semibold tracking-tight text-slate-50">{title}</h3>
            <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-slate-300">{description}</p>
            {actionLabel && onAction ? (
                <div className="mt-7 flex justify-center">
                    <Button variant={actionVariant} onClick={onAction}>
                        {actionLabel}
                    </Button>
                </div>
            ) : null}
        </Card>
    );
}

