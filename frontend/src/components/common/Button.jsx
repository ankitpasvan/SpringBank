'use client'

import { Loader2 } from 'lucide-react'

const variants = {
    primary:
        'bg-brand-600 text-white hover:bg-brand-700 focus-visible:ring-brand-600 disabled:bg-brand-300',
    secondary:
        'bg-white text-ink border border-slate-200 hover:border-slate-300 hover:bg-slate-50 disabled:text-slate-400',
    dark: 'bg-ink text-white hover:bg-slate-800 disabled:bg-slate-400',
    danger: 'bg-red-600 text-white hover:bg-red-700 disabled:bg-red-300',
    lime: 'bg-lime-accent text-ink hover:brightness-95 disabled:opacity-60',
}

const sizes = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-5 py-2.5 text-sm',
    lg: 'px-6 py-3 text-base',
}

export default function Button({
                                   children,
                                   variant = 'primary',
                                   size = 'md',
                                   loading = false,
                                   disabled = false,
                                   className = '',
                                   type = 'button',
                                   ...props
                               }) {
    return (
        <button
            type={type}
            disabled={disabled || loading}
            className={`inline-flex items-center justify-center gap-2 rounded-full font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:cursor-not-allowed ${variants[variant]} ${sizes[size]} ${className}`}
            {...props}
        >
            {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
            {children}
        </button>
    )
}
