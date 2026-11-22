'use client'

import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react'
import { useToast } from '@/context/ToastContext.jsx'

const styles = {
    success: { box: 'border-green-200 bg-white', Icon: CheckCircle2, iconClass: 'text-green-600' },
    error: { box: 'border-red-200 bg-white', Icon: AlertCircle, iconClass: 'text-red-500' },
    info: { box: 'border-sky-200 bg-white', Icon: Info, iconClass: 'text-sky-500' },
}

// Rendered once inside AppLayout / PublicLayout so toasts work everywhere.
export default function Toast() {
    const { toasts, dismiss } = useToast()
    if (!toasts.length) return null

    return (
        <div className="pointer-events-none fixed bottom-4 right-4 z-[60] flex w-[calc(100%-2rem)] max-w-sm flex-col gap-2" aria-live="polite">
            {toasts.map((toast) => {
                const { box, Icon, iconClass } = styles[toast.type] || styles.info
                return (
                    <div
                        key={toast.id}
                        role="status"
                        className={`pointer-events-auto flex items-start gap-2.5 rounded-xl border px-4 py-3 text-sm font-medium text-slate-800 shadow-lift ${box}`}
                    >
                        <Icon className={`mt-0.5 h-4 w-4 shrink-0 ${iconClass}`} aria-hidden="true" />
                        <span className="min-w-0 flex-1">{toast.message}</span>
                        <button
                            type="button"
                            onClick={() => dismiss(toast.id)}
                            aria-label="Dismiss notification"
                            className="rounded-full p-0.5 text-slate-400 hover:text-slate-700"
                        >
                            <X className="h-4 w-4" aria-hidden="true" />
                        </button>
                    </div>
                )
            })}
        </div>
    )
}
