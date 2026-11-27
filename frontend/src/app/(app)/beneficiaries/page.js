'use client'

import { useCallback, useEffect, useState } from 'react'
import { CheckCircle2, Trash2, Users } from 'lucide-react'
import api, { getErrorMessage, getFieldErrors } from '@/lib/api.js'
import { createBeneficiary, deleteBeneficiary, listBeneficiaries, verifyBeneficiary } from '@/services/beneficiaryService.js'
import { CURRENCIES } from '@/lib/currency.js'

const EMPTY = { nickname: '', legalName: '', currency: 'INR', country: 'India', accountNumber: '', ifsc: '', bankName: '', wiseTag: '' }

export default function BeneficiariesPage() {
    const [items, setItems] = useState([])
    const [accounts, setAccounts] = useState([])
    const [form, setForm] = useState(EMPTY)
    const [errors, setErrors] = useState({})
    const [message, setMessage] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)

    const load = useCallback(async () => {
        setLoading(true)
        setError('')
        try {
            const [b, a] = await Promise.all([listBeneficiaries(), api.get('/accounts').then((r) => r.data)])
            setItems(b)
            setAccounts(a)
        } catch (e) {
            setError(getErrorMessage(e))
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => { load() }, [load])

    const submit = async (e) => {
        e.preventDefault()
        setErrors({})
        setError('')
        setMessage('')
        setSaving(true)
        try {
            const created = await createBeneficiary({ ...form, ifsc: form.ifsc || null, bankName: form.bankName || null, wiseTag: form.wiseTag || null })
            setItems((prev) => [created, ...prev])
            setForm(EMPTY)
            setMessage('Beneficiary added. Verify it to use it for transfers.')
        } catch (err) {
            setErrors(getFieldErrors(err))
            setError(getErrorMessage(err))
        } finally {
            setSaving(false)
        }
    }

    const onVerify = async (id) => {
        setError('')
        try {
            const updated = await verifyBeneficiary(id)
            setItems((prev) => prev.map((b) => (b.id === id ? updated : b)))
            setMessage('Beneficiary verified (mock verification).')
        } catch (err) {
            setError(getErrorMessage(err))
        }
    }

    const onDelete = async (id) => {
        setError('')
        try {
            await deleteBeneficiary(id)
            setItems((prev) => prev.filter((b) => b.id !== id))
        } catch (err) {
            setError(getErrorMessage(err))
        }
    }

    return (
        <div>
            <div className="flex items-center gap-3">
                <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-600"><Users className="h-5 w-5" aria-hidden="true" /></span>
                <div>
                    <h1 className="text-2xl font-extrabold tracking-tight">Beneficiaries</h1>
                    <p className="text-sm text-slate-500">Save people and businesses once, then send money to them in one tap.</p>
                </div>
            </div>

            {message && <p className="mt-4 rounded-xl bg-brand-50 px-4 py-3 text-sm font-semibold text-brand-700">{message}</p>}
            {error && <p className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">{error}</p>}

            <form onSubmit={submit} className="mt-6 rounded-3xl border border-slate-100 bg-white p-6 shadow-card">
                <h2 className="text-base font-bold">Add a beneficiary</h2>
                <div className="mt-4 grid gap-4 sm:grid-cols-2">
                    <label className="block text-sm font-semibold">Nickname
                        <input value={form.nickname} onChange={(e) => setForm((f) => ({ ...f, nickname: e.target.value }))} required maxLength={100} className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2.5 outline-none focus:border-brand-600" placeholder="e.g. Mom" />
                        {errors.nickname && <span className="text-xs font-normal text-red-600">{errors.nickname}</span>}
                    </label>
                    <label className="block text-sm font-semibold">Legal name
                        <input value={form.legalName} onChange={(e) => setForm((f) => ({ ...f, legalName: e.target.value }))} required maxLength={100} className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2.5 outline-none focus:border-brand-600" placeholder="Full name as on bank account" />
                        {errors.legalName && <span className="text-xs font-normal text-red-600">{errors.legalName}</span>}
                    </label>
                    <label className="block text-sm font-semibold">Currency
                        <select value={form.currency} onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value }))} className="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 outline-none">
                            {CURRENCIES.map((c) => <option key={c.code} value={c.code}>{c.code} — {c.name}</option>)}
                        </select>
                    </label>
                    <label className="block text-sm font-semibold">Country
                        <input value={form.country} onChange={(e) => setForm((f) => ({ ...f, country: e.target.value }))} required maxLength={100} className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2.5 outline-none focus:border-brand-600" />
                    </label>
                    <label className="block text-sm font-semibold">Account number
                        <input value={form.accountNumber} onChange={(e) => setForm((f) => ({ ...f, accountNumber: e.target.value }))} required minLength={9} maxLength={34} className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2.5 outline-none focus:border-brand-600" placeholder="9–34 characters" />
                        {errors.accountNumber && <span className="text-xs font-normal text-red-600">{errors.accountNumber}</span>}
                    </label>
                    <label className="block text-sm font-semibold">IFSC (India, optional)
                        <input value={form.ifsc} onChange={(e) => setForm((f) => ({ ...f, ifsc: e.target.value.toUpperCase() }))} maxLength={11} className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2.5 outline-none focus:border-brand-600" placeholder="SBIN0123456" />
                        {errors.ifsc && <span className="text-xs font-normal text-red-600">{errors.ifsc}</span>}
                    </label>
                    <label className="block text-sm font-semibold">Bank name (optional)
                        <input value={form.bankName} onChange={(e) => setForm((f) => ({ ...f, bankName: e.target.value }))} maxLength={100} className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2.5 outline-none focus:border-brand-600" />
                    </label>
                    <label className="block text-sm font-semibold">Wisetag / email (optional)
                        <input value={form.wiseTag} onChange={(e) => setForm((f) => ({ ...f, wiseTag: e.target.value }))} maxLength={100} className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2.5 outline-none focus:border-brand-600" placeholder="@username or email" />
                    </label>
                </div>
                <button type="submit" disabled={saving} className="mt-5 rounded-full bg-brand-600 px-6 py-3 font-semibold text-white transition hover:bg-brand-700 disabled:opacity-60">
                    {saving ? 'Adding…' : 'Add beneficiary'}
                </button>
                <p className="mt-3 text-xs text-slate-400">Your accounts on file: {accounts.length ? accounts.map((a) => a.accountNumber).join(', ') : 'none yet — create an account first.'}</p>
            </form>

            <div className="mt-6 space-y-3">
                {loading && <p className="rounded-2xl border border-slate-100 bg-white p-5 text-slate-500">Loading beneficiaries…</p>}
                {!loading && items.length === 0 && <p className="rounded-2xl border border-slate-100 bg-white p-5 text-slate-500">No beneficiaries yet. Add your first one above.</p>}
                {items.map((b) => (
                    <article key={b.id} className="flex flex-wrap items-center gap-4 rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
                        <div className="min-w-0 flex-1">
                            <p className="font-bold text-ink">{b.nickname} <span className="font-normal text-slate-500">— {b.legalName}</span></p>
                            <p className="mt-1 text-sm text-slate-500">{b.currency} · {b.country} · {b.accountNumber}{b.ifsc ? ` · IFSC ${b.ifsc}` : ''}{b.bankName ? ` · ${b.bankName}` : ''}</p>
                            <p className="mt-1 text-xs font-semibold">
                                {b.verified ? <span className="inline-flex items-center gap-1 text-brand-700"><CheckCircle2 className="h-3.5 w-3.5" aria-hidden="true" /> Verified (mock)</span> : <span className="text-amber-700">Not verified yet</span>}
                            </p>
                        </div>
                        {!b.verified && <button type="button" onClick={() => onVerify(b.id)} className="rounded-full bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700">Verify</button>}
                        <button type="button" onClick={() => onDelete(b.id)} className="inline-flex items-center gap-1.5 rounded-full border border-red-200 px-4 py-2 text-sm font-semibold text-red-600 hover:bg-red-50" aria-label={`Delete ${b.nickname}`}>
                            <Trash2 className="h-4 w-4" aria-hidden="true" /> Delete
                        </button>
                    </article>
                ))}
            </div>
        </div>
    )
}