'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { useParams } from 'next/navigation'
import { ArrowDownLeft, ArrowUpRight, Copy, CreditCard, ReceiptText } from 'lucide-react'
import { useToast } from '@/context/ToastContext.jsx'
import { getAccount } from '@/services/accountService.js'
import { deposit, listTransactions, withdraw } from '@/services/transactionService.js'
import { getErrorMessage, getErrorStatus } from '@/lib/api'
import { formatAmount, formatDateTime } from '@/utils/format'
import { validateAmount, validateDescription } from '@/utils/validation'
import Card from '@/components/common/Card.jsx'
import Button from '@/components/common/Button.jsx'
import Input from '@/components/common/Input.jsx'
import Modal from '@/components/common/Modal.jsx'
import Spinner from '@/components/common/Spinner.jsx'
import Alert from '@/components/common/Alert.jsx'
import EmptyState from '@/components/common/EmptyState.jsx'
import TransactionTable from '@/components/common/TransactionTable.jsx'

export default function AccountDetailPage() {
    const { id } = useParams()
    const toast = useToast()
    const [account, setAccount] = useState(null)
    const [transactions, setTransactions] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [modal, setModal] = useState(null) // 'deposit' | 'withdraw' | null
    const [form, setForm] = useState({ amount: '', description: '' })
    const [formErrors, setFormErrors] = useState({})
    const [submitting, setSubmitting] = useState(false)
    const [copied, setCopied] = useState(false)

    const load = useCallback(async () => {
        setLoading(true)
        setError('')
        try {
            const [acc, page] = await Promise.all([
                getAccount(id),
                listTransactions(id, { page: 0, size: 10 }),
            ])
            setAccount(acc)
            setTransactions(page.content || [])
        } catch (err) {
            setError(getErrorMessage(err))
        } finally {
            setLoading(false)
        }
    }, [id])

    useEffect(() => {
        load()
    }, [load])

    const openModal = (kind) => {
        setForm({ amount: '', description: '' })
        setFormErrors({})
        setModal(kind)
    }

    const handleMoney = async (e) => {
        e.preventDefault()
        const next = {}
        const amountError = validateAmount(form.amount)
        if (amountError) next.amount = amountError
        const descError = validateDescription(form.description)
        if (descError) next.description = descError
        setFormErrors(next)
        if (Object.keys(next).length) return

        setSubmitting(true)
        try {
            const result =
                modal === 'deposit'
                    ? await deposit(id, { amount: form.amount, description: form.description.trim() })
                    : await withdraw(id, { amount: form.amount, description: form.description.trim() })
            toast.success(
                modal === 'deposit'
                    ? `Deposit successful. New balance: ${formatAmount(result.balanceAfter)}.`
                    : `Withdrawal successful. New balance: ${formatAmount(result.balanceAfter)}.`,
            )
            setModal(null)
            await load()
        } catch (err) {
            const status = getErrorStatus(err)
            if (status === 422) {
                toast.error('Insufficient funds — the amount is more than your balance.')
            } else {
                toast.error(getErrorMessage(err))
            }
        } finally {
            setSubmitting(false)
        }
    }

    const copyId = async () => {
        try {
            await navigator.clipboard.writeText(account.id)
            setCopied(true)
            setTimeout(() => setCopied(false), 2000)
        } catch {
            toast.error('Could not copy the account ID.')
        }
    }

    if (loading) return <Spinner label="Loading account…" />

    if (error || !account) {
        return (
            <div className="space-y-6">
                <Alert type="error">{error || 'Account not found.'}</Alert>
                <Link href="/accounts" className="text-sm font-semibold text-brand-700 hover:underline">
                    ← Back to My Accounts
                </Link>
            </div>
        )
    }

    return (
        <div className="space-y-6">
            <Link href="/accounts" className="text-sm font-semibold text-brand-700 hover:underline">
                ← Back to My Accounts
            </Link>

            <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                        {account.accountType === 'SAVINGS' ? 'Savings Account' : 'Current Account'}
                    </h1>
                    <p className="mt-1 font-mono text-sm text-slate-500">Account no. {account.accountNumber}</p>
                </div>
                <div className="flex gap-2">
                    <Button variant="secondary" onClick={() => openModal('deposit')}>
                        <ArrowDownLeft className="h-4 w-4" aria-hidden="true" />
                        Deposit
                    </Button>
                    <Button variant="dark" onClick={() => openModal('withdraw')}>
                        <ArrowUpRight className="h-4 w-4" aria-hidden="true" />
                        Withdraw
                    </Button>
                </div>
            </div>

            <div className="grid gap-4 lg:grid-cols-3">
                <Card className="bg-ink p-6 text-white lg:col-span-2">
                    <p className="text-xs font-semibold uppercase tracking-wide text-white/60">Current balance</p>
                    <p className="mt-2 text-4xl font-extrabold tracking-tight">{formatAmount(account.balance)}</p>
                    <p className="mt-2 text-sm text-white/60">Updated {formatDateTime(account.updatedAt)}</p>
                </Card>
                <Card className="p-6">
                    <h2 className="flex items-center gap-2 text-base font-bold">
                        <CreditCard className="h-5 w-5 text-brand-600" aria-hidden="true" />
                        Account details
                    </h2>
                    <dl className="mt-4 space-y-3 text-sm">
                        <div className="flex justify-between gap-3">
                            <dt className="text-slate-500">Account number</dt>
                            <dd className="font-mono font-semibold text-ink">{account.accountNumber}</dd>
                        </div>
                        <div className="flex justify-between gap-3">
                            <dt className="text-slate-500">Type</dt>
                            <dd className="font-semibold text-ink">{account.accountType}</dd>
                        </div>
                        <div className="flex justify-between gap-3">
                            <dt className="text-slate-500">Opened</dt>
                            <dd className="font-semibold text-ink">{formatDateTime(account.createdAt)}</dd>
                        </div>
                        <div>
                            <dt className="text-slate-500">Account ID (for receiving transfers)</dt>
                            <dd className="mt-1 flex items-center gap-2">
                                <code className="min-w-0 flex-1 break-all rounded-lg bg-slate-100 px-2 py-1.5 font-mono text-xs text-ink">
                                    {account.id}
                                </code>
                                <button
                                    type="button"
                                    onClick={copyId}
                                    aria-label="Copy account ID"
                                    className="shrink-0 rounded-full p-2 text-slate-500 hover:bg-slate-100 hover:text-ink"
                                >
                                    <Copy className="h-4 w-4" aria-hidden="true" />
                                </button>
                            </dd>
                            {copied && <p className="mt-1 text-xs font-medium text-brand-700">Account ID copied.</p>}
                            <p className="mt-1.5 text-xs leading-relaxed text-slate-500">
                                Share this ID with anyone sending you money — transfers use the account ID, not the
                                12-digit account number.
                            </p>
                        </div>
                    </dl>
                </Card>
            </div>

            <Card className="p-5 sm:p-6">
                <div className="flex items-center justify-between">
                    <h2 className="text-lg font-bold">Recent transactions</h2>
                    <Link href={`/transactions?accountId=${account.id}`} className="text-sm font-semibold text-brand-700 hover:underline">
                        View all
                    </Link>
                </div>
                {transactions.length === 0 ? (
                    <EmptyState
                        icon={ReceiptText}
                        title="No transactions yet"
                        description="Deposit, withdraw or receive a transfer and it will appear here with the balance after each movement."
                    />
                ) : (
                    <div className="mt-2">
                        <TransactionTable transactions={transactions} />
                    </div>
                )}
            </Card>

            <Modal
                open={modal !== null}
                onClose={() => setModal(null)}
                title={modal === 'deposit' ? 'Deposit money' : 'Withdraw money'}
            >
                <form onSubmit={handleMoney} noValidate className="space-y-5">
                    <Input
                        label="Amount"
                        id="money-amount"
                        type="number"
                        inputMode="decimal"
                        step="0.01"
                        min="0.01"
                        max="1000000"
                        placeholder="0.00"
                        value={form.amount}
                        onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))}
                        error={formErrors.amount}
                        hint="Between 0.01 and 1,000,000.00, up to 2 decimal places."
                    />
                    <Input
                        label="Description (optional)"
                        id="money-description"
                        type="text"
                        placeholder="e.g. Salary, Rent"
                        maxLength={100}
                        value={form.description}
                        onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                        error={formErrors.description}
                    />
                    <p className="rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
                        Current balance: <span className="font-bold text-ink">{formatAmount(account.balance)}</span>
                        {modal === 'withdraw' && ' — withdrawals above your balance are rejected by the bank.'}
                    </p>
                    <Button type="submit" loading={submitting} className="w-full" size="lg">
                        {modal === 'deposit' ? 'Deposit' : 'Withdraw'}
                    </Button>
                </form>
            </Modal>
        </div>
    )
}
