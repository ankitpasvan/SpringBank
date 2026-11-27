'use client'

import { useState } from 'react'
import PublicLayout from '@/components/layout/PublicLayout.jsx'

const FAQS = [
    { q: 'Is SpringBank a real bank?', a: 'No. SpringBank is a demo banking application built on Spring Boot. No real money moves and no real international remittance happens here.' },
    { q: 'How do I send money?', a: 'Log in, go to Send Money, choose a source account, enter the destination account ID, amount and an optional description, then confirm. Transfers are idempotent — the same request is never applied twice.' },
    { q: 'How are exchange rates calculated?', a: 'Rates come from the free Frankfurter / ECB feed via the SpringBank backend, with a static fallback. They are indicative only, not transactable rates.' },
    { q: 'What fee do I pay?', a: 'The demo fee is 0.49% of the amount you send, shown before you confirm.' },
    { q: 'I forgot my password. What do I do?', a: 'Password reset is not built yet — it is in the security batch. For now, register a new demo account with a different email.' },
    { q: 'Is my session secure?', a: 'Yes. Sign-in uses JWT with a 1-hour expiry, passwords are hashed with BCrypt, and you can only access your own accounts.' },
]

export default function HelpPage() {
    const [query, setQuery] = useState('')
    const filtered = FAQS.filter((f) => (f.q + f.a).toLowerCase().includes(query.toLowerCase()))
    return (
        <PublicLayout>
            <div className="mx-auto max-w-4xl px-4 py-14 sm:px-6 lg:px-8">
                <h1 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Help centre</h1>
                <p className="mt-3 text-lg text-slate-600">Search answers, or browse the questions below.</p>
                <input
                    type="search" value={query} onChange={(e) => setQuery(e.target.value)}
                    placeholder="Search help…" aria-label="Search help"
                    className="mt-6 w-full rounded-full border border-slate-200 px-5 py-3 text-ink outline-none focus:border-brand-600"
                />
                <div className="mt-8 space-y-4">
                    {filtered.length === 0 && <p className="rounded-2xl border border-slate-100 bg-white p-5 text-slate-600">No answers match “{query}”. Try a different word.</p>}
                    {filtered.map((f) => (
                        <details key={f.q} className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
                            <summary className="cursor-pointer font-bold text-ink">{f.q}</summary>
                            <p className="mt-2 text-sm leading-relaxed text-slate-600">{f.a}</p>
                        </details>
                    ))}
                </div>
                <p className="mt-8 text-sm text-slate-500">Still stuck? This demo has no live support chat. Check the FAQ above or the pricing and converter pages.</p>
            </div>
        </PublicLayout>
    )
}