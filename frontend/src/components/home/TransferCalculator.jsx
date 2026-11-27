'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { ArrowLeftRight, ArrowUpRight } from 'lucide-react'
import { CURRENCIES, formatAmount, recipientGets } from '@/lib/currency.js'
import { fetchRate } from '@/lib/fx.js'

export default function TransferCalculator() {
    const [amount, setAmount] = useState('80000')
    const [from, setFrom] = useState('INR')
    const [to, setTo] = useState('USD')
    const [rate, setRate] = useState(null)
    const [source, setSource] = useState('')
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        let cancelled = false
        setLoading(true)
        fetchRate(from, to)
            .then(({ rate: r, source: s }) => {
                if (!cancelled) { setRate(r); setSource(s); setLoading(false) }
            })
            .catch(() => { if (!cancelled) setLoading(false) })
        return () => { cancelled = true }
    }, [from, to])

    const num = Number(amount || 0)
    const { fee, gets } = rate ? recipientGets(num, rate) : { fee: 0, gets: 0 }
    const swap = () => { setFrom(to); setTo(from) }

    return (
        <div className="rounded-3xl border border-slate-100 bg-white p-5 shadow-lift sm:p-6">
            <div className="flex items-center justify-between">
                <p className="text-sm font-bold text-ink">Send money abroad</p>
                <span className="rounded-full bg-lime-accent/60 px-3 py-1 text-xs font-bold text-ink">Demo</span>
            </div>
            <label className="mt-5 block text-xs font-semibold text-slate-500" htmlFor="calc-amount">You send exactly</label>
            <div className="mt-1 flex gap-2">
                <input
                    id="calc-amount"
                    type="number" min="0" value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className="min-w-0 flex-1 rounded-xl border border-slate-200 px-3 py-3 text-lg font-bold text-ink outline-none focus:border-brand-600"
                />
                <select value={from} onChange={(e) => setFrom(e.target.value)} className="rounded-xl border border-slate-200 bg-white px-3 py-3 font-semibold text-ink outline-none" aria-label="From currency">
                    {CURRENCIES.map((c) => <option key={c.code} value={c.code}>{c.code}</option>)}
                </select>
            </div>
            <button type="button" onClick={swap} className="mt-3 inline-flex items-center gap-1.5 rounded-full bg-brand-50 px-3 py-1.5 text-xs font-bold text-brand-700 hover:bg-brand-100" aria-label="Swap currencies">
                <ArrowLeftRight className="h-3.5 w-3.5" aria-hidden="true" /> Swap
            </button>
            <label className="mt-3 block text-xs font-semibold text-slate-500" htmlFor="calc-to">Recipient gets</label>
            <div className="mt-1 flex gap-2">
                <input id="calc-to" readOnly value={loading ? '…' : formatAmount(gets, to)} className="min-w-0 flex-1 rounded-xl border border-slate-200 bg-mist px-3 py-3 text-lg font-bold text-ink outline-none" />
                <select value={to} onChange={(e) => setTo(e.target.value)} className="rounded-xl border border-slate-200 bg-white px-3 py-3 font-semibold text-ink outline-none" aria-label="To currency">
                    {CURRENCIES.map((c) => <option key={c.code} value={c.code}>{c.code}</option>)}
                </select>
            </div>
            <dl className="mt-4 space-y-2 text-sm">
                <div className="flex justify-between"><dt className="text-slate-500">Exchange rate</dt><dd className="font-semibold text-ink">{loading ? '…' : `1 ${from} = ${Number(rate).toFixed(4)} ${to}`}</dd></div>
                <div className="flex justify-between"><dt className="text-slate-500">Fee (0.49% demo)</dt><dd className="font-semibold text-ink">{formatAmount(fee, from)}</dd></div>
                <div className="flex justify-between"><dt className="text-slate-500">Arrives by</dt><dd className="font-semibold text-ink">Within 1 working day</dd></div>
            </dl>
            <p className="mt-3 text-xs leading-relaxed text-slate-400">Indicative rate via {source || 'SpringBank API'}. Demo only — no real money moves.</p>
            <Link href="/register" className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-full bg-brand-600 px-6 py-3 font-semibold text-white transition hover:bg-brand-700">
                Send money <ArrowUpRight className="h-4 w-4" aria-hidden="true" />
            </Link>
        </div>
    )
}