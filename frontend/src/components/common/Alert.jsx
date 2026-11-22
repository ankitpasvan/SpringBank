'use client'

import { AlertCircle, CheckCircle2, Info } from 'lucide-react'

const styles = {
    error: { box: 'border-red-200 bg-red-50 text-red-800', Icon: AlertCircle, iconClass: 'text-red-500' },
    success: { box: 'border-green-200 bg-green-50 text-green-800', Icon: CheckCircle2, iconClass: 'text-green-600' },
    info: { box: 'border-sky-200 bg-sky-50 text-sky-800', Icon: Info, iconClass: 'text-sky-500' },
}

export default function Alert({ type = 'info', children, className = '' }) {
    const { box, Icon, iconClass } = styles[type] || styles.info
    return (
        <div role="alert" className={`flex items-start gap-2.5 rounded-xl border px-4 py-3 text-sm ${box} ${className}`}>
            <Icon className={`mt-0.5 h-4 w-4 shrink-0 ${iconClass}`} aria-hidden="true" />
            <div className="min-w-0">{children}</div>
        </div>
    )
}
