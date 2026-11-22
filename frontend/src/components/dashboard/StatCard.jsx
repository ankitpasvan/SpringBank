'use client'

import Card from '@/components/common/Card.jsx'

export default function StatCard({ icon: Icon, label, value, sub, tone = 'green' }) {
    const tones = {
        green: 'bg-brand-50 text-brand-600',
        dark: 'bg-ink/5 text-ink',
        lime: 'bg-lime-accent/40 text-ink',
        red: 'bg-red-50 text-red-500',
    }
    return (
        <Card className="p-5">
            <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-500">{label}</p>
                    <p className="mt-1.5 truncate text-2xl font-extrabold tracking-tight text-ink">{value}</p>
                    {sub && <p className="mt-1 text-xs leading-relaxed text-slate-500">{sub}</p>}
                </div>
                {Icon && (
                    <span className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${tones[tone] || tones.green}`}>
            <Icon className="h-5 w-5" aria-hidden="true" />
          </span>
                )}
            </div>
        </Card>
    )
}
