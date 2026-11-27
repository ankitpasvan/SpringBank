'use client'

import { useEffect, useState } from 'react'
import { ArrowLeftRight } from 'lucide-react'
import PublicLayout from '@/components/layout/PublicLayout.jsx'
import { CURRENCIES, formatAmount, recipientGets } from '@/lib/currency.js'
import { fetchRates } from '@/lib/fx.js'

export default function ConverterPage() {
    const [amount, setAmount] = useState('10000')
    const [from, setFrom] = useState('INR')
    const [to, setTo] = useState('USD')
    const [rates, setRates] = useState(null)
    const [source, setSource] = useState('')
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        let cancelled = false
        setLoading(true)
        fetchRates(from)
            .then((data) => { if (!cancelled) { setRates(data.rates); setSource(data.source); setLoading(false) } })
            .catch(() => { if (!cancelled) setLoading(false) })
        return () => { cancelled = true }
    }, [from])

    const rate = rates ? Number(rates[to] || 0) : 0
    const { fee, gets } = recipientGets(Number(amount || 0), rate)
    const swap = () => { setFrom(to); setTo(from) }

    return (
        <PublicLayout>
            <div className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
                <h1 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Currency converter</h1>
                <p className="mt-3 max-w-2xl text-lg text-slate-600">Convert between INR, USD, EUR, GBP, AUD, CAD and SGD at an indicative mid-market rate.</p>
                <div className="mt-10 grid gap-8 lg:grid-cols-2">
                    <div className="rounded-3xl border border-slate-100 bg-white p-6 shadow-card">
                        <label className="block text-xs font-semibold text-slate-500" htmlFor="conv-amount">Amount</label>
                        <div className="mt-1 flex gap-2">
                            <input id="conv-amount" type="number" min="0" value={amount} onChange={(e) => setAmount(e.target.value)} className="min-w-0 flex-1 rounded-xl border border-slate-200 px-3 py-3 text-lg font-bold text-ink outline-none focus:border-brand-600" />
                            <select value={from} onChange={(e) => setFrom(e.target.value)} className="rounded-xl border border-slate-200 bg-white px-3 py-3 font-semibold text-ink outline-none" aria-label="From currency">
                                {CURRENCIES.map((c) => <option key={c.code} value={c.code}>{c.code} — {c.name}</option>)}
                            </select>
                        </div>
                        <button type="button" onClick={swap} className="mt-3 inline-flex items-center gap-1.5 rounded-full bg-brand-50 px-3 py-1.5 text-xs font-bold text-brand-700 hover:bg-brand-100" aria-label="Swap currencies">
                            <ArrowLeftRight className="h-3.5 w-3.5" aria-hidden="true" /> Swap
                        </button>
                        <label className="mt-3 block text-xs font-semibold text-slate-500" htmlFor="conv-to">Converted amount</label>
                        <div className="mt-1 flex gap-2">
                            <input id="conv-to" readOnly value={loading ? '…' : formatAmount(gets, to)} className="min-w-0 flex-1 rounded-xl border border-slate-200 bg-mist px-3 py-3 text-lg font-bold text-ink outline-none" />
                            <select value={to} onChange={(e) => setTo(e.target.value)} className="rounded-xl border border-slate-200 bg-white px-3 py-3 font-semibold text-ink outline-none" aria-label="To currency">
                                {CURRENCIES.map((c) => <option key={c.code} value={c.code}>{c.code} — {c.name}</option>)}
                            </select>
                        </div>
                        <dl className="mt-5 space-y-2 text-sm">
                            <div className="flex justify-between"><dt className="text-slate-500">Rate</dt><dd className="font-semibold text-ink">{loading ? '…' : `1 ${from} = ${rate.toFixed(4)} ${to}`}</dd></div>
                            <div className="flex justify-between"><dt className="text-slate-500">Fee (0.49% demo)</dt><dd className="font-semibold text-ink">{formatAmount(fee, from)}</dd></div>
                            <div className="flex justify-between"><dt className="text-slate-500">Source</dt><dd className="font-semibold text-ink">{source || '—'}</dd></div>
                        </dl>
                        <p className="mt-3 text-xs leading-relaxed text-slate-400">Indicative rate only, not a transactable rate. Demo — no real money moves.</p>
                    </div>
                    <div className="rounded-3xl border border-slate-100 bg-mist p-6">
                        <h2 className="text-lg font-bold">All rates from {from}</h2>
                        <ul className="mt-4 space-y-3">
                            {CURRENCIES.filter((c) => c.code !== from).map((c) => (
                                <li key={c.code} className="flex items-center justify-between rounded-2xl border border-slate-100 bg-white px-4 py-3">
                                    <span className="font-semibold text-ink">{c.code} <span className="font-normal text-slate-500">— {c.name}</span></span>
                                    <span className="font-bold text-ink">{loading || !rates ? '…' : Number(rates[c.code] || 0).toFixed(4)}</span>
                                </li>
                            ))}
                        </ul>
                    </div>
                </div>
            </div>
        </PublicLayout>
    )
}