'use client'

import { useEffect } from 'react'
import { X } from 'lucide-react'

export default function Modal({ open, onClose, title, children }) {
    useEffect(() => {
        if (!open) return
        const onKey = (e) => {
            if (e.key === 'Escape') onClose()
        }
        document.addEventListener('keydown', onKey)
        document.body.style.overflow = 'hidden'
        return () => {
            document.removeEventListener('keydown', onKey)
            document.body.style.overflow = ''
        }
    }, [open, onClose])

    if (!open) return null

    return (
        <div
            className="fixed inset-0 z-50 flex items-end justify-center bg-ink/50 p-0 sm:items-center sm:p-6"
            role="dialog"
            aria-modal="true"
            aria-label={title}
            onClick={onClose}
        >
            <div
                className="w-full max-w-md rounded-t-2xl bg-white p-6 shadow-lift sm:rounded-2xl"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="mb-4 flex items-start justify-between gap-4">
                    <h2 className="text-lg font-bold">{title}</h2>
                    <button
                        type="button"
                        onClick={onClose}
                        aria-label="Close dialog"
                        className="rounded-full p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-800"
                    >
                        <X className="h-5 w-5" aria-hidden="true" />
                    </button>
                </div>
                {children}
            </div>
        </div>
    )
}
