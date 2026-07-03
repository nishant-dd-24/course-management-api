export default function PageHeader({ title, subtitle, actions, eyebrow }) {
    return (
        <div className="flex flex-col gap-4 border-b border-slate-800/80 pb-6 md:flex-row md:items-end md:justify-between">
            <div className="space-y-3">
                {eyebrow ? <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">{eyebrow}</p> : null}
                <h1 className="text-3xl font-semibold tracking-tight text-slate-50 md:text-[2.25rem]">{title}</h1>
                {subtitle ? <p className="max-w-3xl text-sm leading-6 text-slate-300 md:text-base">{subtitle}</p> : null}
            </div>

            {actions ? <div className="flex flex-wrap gap-2 md:justify-end">{actions}</div> : null}
        </div>
    );
}

