'use client'

export default function EmptyState({ icon: Icon, title, description, action }) {
    return (
        <div className="flex flex-col items-center justify-center px-6 py-12 text-center">
            {Icon && (
                <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-50 text-brand-600">
                    <Icon className="h-7 w-7" aria-hidden="true" />
                </div>
            )}
            <h3 className="text-base font-bold">{title}</h3>
            {description && <p className="mt-1 max-w-sm text-sm leading-relaxed text-slate-500">{description}</p>}
            {action && <div className="mt-5">{action}</div>}
        </div>
    )
}
