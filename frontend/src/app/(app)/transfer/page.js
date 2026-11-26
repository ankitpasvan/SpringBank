'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { ArrowLeftRight, CheckCircle2 } from 'lucide-react'
import { useToast } from '@/context/ToastContext.jsx'
import { listAccounts } from '@/services/accountService.js'
import { transfer } from '@/services/transferService.js'
import { getErrorMessage, getErrorStatus } from '@/lib/api'
import { formatAmount, formatDateTime } from '@/utils/format'
import { validateAmount, validateDescription, validateUuid } from '@/utils/validation'
import Card from '@/components/common/Card.jsx'
import Button from '@/components/common/Button.jsx'
import Input from '@/components/common/Input.jsx'
import Alert from '@/components/common/Alert.jsx'
import Spinner from '@/components/common/Spinner.jsx'
import EmptyState from '@/components/common/EmptyState.jsx'

export default function TransferPage() {
    const toast = useToast()
    const router = useRouter()
    const [accounts, setAccounts] = useState([])
    const [loading, setLoading] = useState(true)
    const [fromAccountId, setFromAccountId] = useState('')
    const [form, setForm] = useState({ toAccountId: '', amount: '', description: '' })
    const [errors, setErrors] = useState({})
    const [submitting, setSubmitting] = useState(false)
    const [result, setResult] = useState(null)

    useEffect(() => {
        async function load() {
            try {
                const list = await listAccounts()
                setAccounts(list)
                if (list.length > 0) setFromAccountId(list[0].id)
            } catch (err) {
                toast.error(getErrorMessage(err))
            } finally {
                setLoading(false)
            }
        }
        load()
    }, []) // eslint-disable-line react-hooks/exhaustive-deps

    const fromAccount = accounts.find((a) => a.id === fromAccountId)

    const handleSubmit = async (e) => {
        e.preventDefault()
        const next = {}
        const toError = validateUuid(form.toAccountId)
        if (toError) next.toAccountId = toError
        else if (form.toAccountId.trim() === fromAccountId) {
            next.toAccountId = 'You cannot transfer to the same account.'
        }
        const amountError = validateAmount(form.amount)
        if (amountError) next.amount = amountError
        const descError = validateDescription(form.description)
        if (descError) next.description = descError
        setErrors(next)
        if (Object.keys(next).length) return

        setSubmitting(true)
        try {
            const res = await transfer(fromAccountId, {
                toAccountId: form.toAccountId.trim(),
                amount: form.amount,
                description: form.description.trim(),
            })
            setResult(res)
            toast.success('Transfer completed successfully.')
        } catch (err) {
            const status = getErrorStatus(err)
            if (status === 422) {
                toast.error('Insufficient funds — the amount is more than your balance.')
            } else if (status === 404) {
                toast.error('Destination account not found. Check the account ID and try again.')
            } else if (status === 409) {
                toast.error('This transfer was already processed. Please check your transactions.')
            } else {
                toast.error(getErrorMessage(err))
            }
        } finally {
            setSubmitting(false)
        }
    }

    const reset = () => {
        setResult(null)
        setForm({ toAccountId: '', amount: '', description: '' })
        setErrors({})
    }

    if (loading) return <Spinner label="Loading your accounts…" />

    return (
        <div className="mx-auto max-w-2xl space-y-6">
            <div>
                <h1 className="text-2xl font-extrabold tracking-tight sm:text-3xl">Send Money</h1>
                <p className="mt-1 text-slate-600">Transfer money to another SpringBank account.</p>
            </div>

            {accounts.length === 0 ? (
                <Card>
                    <EmptyState
                        icon={ArrowLeftRight}
                        title="No accounts yet"
                        description="Open an account first, then you can send money from it."
                        action={
                            <Button onClick={() => router.push('/accounts')}>Open an account</Button>
                        }
                    />
                </Card>
            ) : result ? (
                <Card className="p-6 sm:p-8">
                    <div className="flex flex-col items-center text-center">
            <span className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-50 text-brand-600">
              <CheckCircle2 className="h-8 w-8" aria-hidden="true" />
            </span>
                        <h2 className="mt-4 text-xl font-extrabold">Transfer successful</h2>
                        <p className="mt-1 text-slate-600">
                            {formatAmount(result.amount)} was sent.
                        </p>
                    </div>
                    <dl className="mt-6 space-y-3 rounded-2xl bg-mist p-5 text-sm">
                        <div className="flex justify-between gap-3">
                            <dt className="text-slate-500">Amount sent</dt>
                            <dd className="font-bold text-ink">{formatAmount(result.amount)}</dd>
                        </div>
                        <div className="flex justify-between gap-3">
                            <dt className="text-slate-500">Your balance after</dt>
                            <dd className="font-bold text-ink">{formatAmount(result.senderBalanceAfter)}</dd>
                        </div>
                        <div className="flex justify-between gap-3">
                            <dt className="text-slate-500">Date</dt>
                            <dd className="font-semibold text-ink">{formatDateTime(result.createdAt)}</dd>
                        </div>
                        {result.description && (
                            <div className="flex justify-between gap-3">
                                <dt className="text-slate-500">Description</dt>
                                <dd className="font-semibold text-ink">{result.description}</dd>
                            </div>
                        )}
                    </dl>
                    <div className="mt-6 flex flex-col gap-2 sm:flex-row">
                        <Button onClick={reset} className="flex-1">Send another transfer</Button>
                        <Button variant="secondary" onClick={() => router.push('/transactions')} className="flex-1">
                            View transactions
                        </Button>
                    </div>
                </Card>
            ) : (
                <Card className="p-6 sm:p-8">
                    <form onSubmit={handleSubmit} noValidate className="space-y-5">
                        <div>
                            <label htmlFor="transfer-from" className="mb-1.5 block text-sm font-semibold text-ink">
                                From account
                            </label>
                            <select
                                id="transfer-from"
                                value={fromAccountId}
                                onChange={(e) => setFromAccountId(e.target.value)}
                                className="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-[15px] focus:border-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-600/20"
                            >
                                {accounts.map((a) => (
                                    <option key={a.id} value={a.id}>
                                        {a.accountType} · {a.accountNumber} · balance {formatAmount(a.balance)}
                                    </option>
                                ))}
                            </select>
                            {fromAccount && (
                                <p className="mt-1.5 text-xs text-slate-500">
                                    Available balance: <span className="font-bold text-ink">{formatAmount(fromAccount.balance)}</span>
                                </p>
                            )}
                        </div>

                        <Input
                            label="Destination account ID"
                            id="transfer-to"
                            type="text"
                            placeholder="e.g. 123e4567-e89b-12d3-a456-426614174000"
                            value={form.toAccountId}
                            onChange={(e) => setForm((f) => ({ ...f, toAccountId: e.target.value }))}
                            error={errors.toAccountId}
                            hint="Enter the destination account's UUID (shown on its detail page). This is NOT the 12-digit account number — transfers using the account number will fail."
                        />

                        <Input
                            label="Amount"
                            id="transfer-amount"
                            type="number"
                            inputMode="decimal"
                            step="0.01"
                            min="0.01"
                            max="1000000"
                            placeholder="0.00"
                            value={form.amount}
                            onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))}
                            error={errors.amount}
                            hint="Between 0.01 and 1,000,000.00, up to 2 decimal places."
                        />

                        <Input
                            label="Description (optional)"
                            id="transfer-description"
                            type="text"
                            placeholder="e.g. Rent for November"
                            maxLength={100}
                            value={form.description}
                            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                            error={errors.description}
                        />

                        <Alert type="info">
                            Transfers are checked by the bank: sending to your own account is rejected, and any
                            transfer above your balance is refused. Each transfer is protected against accidental
                            double-submission.
                        </Alert>

                        <Button type="submit" loading={submitting} className="w-full" size="lg">
                            <ArrowLeftRight className="h-4 w-4" aria-hidden="true" />
                            Send transfer
                        </Button>
                    </form>
                </Card>
            )}
        </div>
    )
}