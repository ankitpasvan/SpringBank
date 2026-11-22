'use client'

import { Loader2 } from 'lucide-react'

export default function Spinner({ label = 'Loading…', className = '' }) {
    return (
        <div className={`flex items-center justify-center gap-2 py-10 text-slate-500 ${className}`} role="status" aria-live="polite">
            <Loader2 className="h-5 w-5 animate-spin text-brand-600" aria-hidden="true" />
            <span className="text-sm font-medium">{label}</span>
        </div>
    )
}
