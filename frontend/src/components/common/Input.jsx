'use client'

export default function Input({
                                  label,
                                  id,
                                  error,
                                  hint,
                                  className = '',
                                  ...props
                              }) {
    return (
        <div className="w-full">
            {label && (
                <label htmlFor={id} className="mb-1.5 block text-sm font-semibold text-ink">
                    {label}
                </label>
            )}
            <input
                id={id}
                aria-invalid={Boolean(error)}
                aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined}
                className={`w-full rounded-xl border bg-white px-4 py-2.5 text-[15px] text-slate-900 placeholder:text-slate-400 transition focus:border-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-600/20 disabled:bg-slate-50 disabled:text-slate-500 ${
                    error ? 'border-red-400' : 'border-slate-300'
                } ${className}`}
                {...props}
            />
            {hint && !error && (
                <p id={`${id}-hint`} className="mt-1.5 text-xs leading-relaxed text-slate-500">
                    {hint}
                </p>
            )}
            {error && (
                <p id={`${id}-error`} role="alert" className="mt-1.5 text-xs font-medium text-red-600">
                    {error}
                </p>
            )}
        </div>
    )
}
