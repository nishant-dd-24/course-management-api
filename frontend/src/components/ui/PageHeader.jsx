export default function PageHeader({ title, subtitle, actions, eyebrow }) {
    return (
        <div className="flex flex-col gap-4 border-b border-zinc-800/80 pb-6 md:flex-row md:items-end md:justify-between">
            <div className="space-y-2">
                {eyebrow ? <p className="text-[0.7rem] font-semibold uppercase tracking-[0.15em] text-blue-500/80">{eyebrow}</p> : null}
                <h1 className="text-3xl font-semibold tracking-tighter text-zinc-50 md:text-4xl">{title}</h1>
                {subtitle ? <p className="max-w-2xl text-sm leading-6 text-zinc-400 md:text-base">{subtitle}</p> : null}
            </div>

            {actions ? <div className="flex flex-wrap gap-3 md:justify-end">{actions}</div> : null}
        </div>
    );
}

