'use client'

export default function Card({ children, className = '' }) {
    return (
        <div className={`rounded-2xl border border-slate-100 bg-white shadow-card ${className}`}>
            {children}
        </div>
    )
}
