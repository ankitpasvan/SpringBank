'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { ArrowDownLeft, ArrowLeftRight, ArrowUpRight, CreditCard, Plus, ReceiptText, Wallet } from 'lucide-react'
import { useAuth } from '@/context/AuthContext.jsx'
import { listAccounts } from '@/services/accountService.js'
import { listTransactions } from '@/services/transactionService.js'
import { getErrorMessage } from '@/lib/api'
import { formatAmount, maskAccountNumber } from '@/utils/format'
import Card from '@/components/common/Card.jsx'
import Spinner from '@/components/common/Spinner.jsx'
import Alert from '@/components/common/Alert.jsx'
import EmptyState from '@/components/common/EmptyState.jsx'
import TransactionTable from '@/components/common/TransactionTable.jsx'
import StatCard from '@/components/dashboard/StatCard.jsx'
import BalanceChart from '@/components/dashboard/BalanceChart.jsx'

export default function DashboardPage() {
    const { user } = useAuth()
    const [accounts, setAccounts] = useState([])
    const [recent, setRecent] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    const load = useCallback(async () => {
        setLoading(true)
        setError('')
        try {
            const list = await listAccounts()
            setAccounts(list)
            if (list.length > 0) {
                // Recent transactions for the first account (newest first, page 0).
                const page = await listTransactions(list[0].id, { page: 0, size: 5 })
                setRecent(page.content || [])
            } else {
                setRecent([])
            }
        } catch (err) {
            setError(getErrorMessage(err))
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        load()
    }, [load])

    if (loading) return <Spinner label="Loading your dashboard…" />

    const totalBalance = accounts.reduce((sum, a) => sum + (Number(a.balance) || 0), 0)

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-end justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                        Hello, {user?.name?.split(' ')[0] || 'there'}
                    </h1>
                    <p className="mt-1 text-slate-600">Here is a summary of your money today.</p>
                </div>
                <Link
                    href="/accounts"
                    className="inline-flex items-center gap-2 rounded-full bg-ink px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-slate-800"
                >
                    <Plus className="h-4 w-4" aria-hidden="true" />
                    Open an account
                </Link>
            </div>

            {error && <Alert type="error">{error}</Alert>}

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <StatCard icon={Wallet} label="Total balance" value={formatAmount(totalBalance)} sub={`${accounts.length} account${accounts.length === 1 ? '' : 's'}`} />
                <StatCard
                    icon={CreditCard}
                    label="Accounts"
                    value={String(accounts.length)}
                    sub={accounts.length ? accounts.map((a) => a.accountType).join(' · ') : 'No accounts yet — open your first one.'}
                    tone="dark"
                />
                <StatCard
                    icon={ReceiptText}
                    label="Recent transactions"
                    value={String(recent.length)}
                    sub={recent.length ? 'Showing your latest activity.' : 'No transactions yet.'}
                    tone="lime"
                />
            </div>

            {/* Accounts overview */}
            <Card className="p-5 sm:p-6">
                <div className="flex items-center justify-between">
                    <h2 className="text-lg font-bold">Your accounts</h2>
                    <Link href="/accounts" className="text-sm font-semibold text-brand-700 hover:underline">
                        View all
                    </Link>
                </div>
                {accounts.length === 0 ? (
                    <EmptyState
                        icon={CreditCard}
                        title="No accounts yet"
                        description="Open your first Savings or Current account to get started. It only takes a moment."
                        action={
                            <Link
                                href="/accounts"
                                className="inline-flex items-center gap-2 rounded-full bg-brand-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-brand-700"
                            >
                                <Plus className="h-4 w-4" aria-hidden="true" />
                                Open an account
                            </Link>
                        }
                    />
                ) : (
                    <ul className="mt-4 divide-y divide-slate-100">
                        {accounts.map((a) => (
                            <li key={a.id}>
                                <Link href={`/accounts/${a.id}`} className="flex items-center gap-4 py-3.5 transition hover:bg-slate-50/60">
                  <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                    <CreditCard className="h-5 w-5" aria-hidden="true" />
                  </span>
                                    <div className="min-w-0 flex-1">
                                        <p className="font-semibold text-ink">
                                            {a.accountType === 'SAVINGS' ? 'Savings Account' : 'Current Account'}
                                        </p>
                                        <p className="text-sm text-slate-500">•••• {maskAccountNumber(a.accountNumber).slice(-4)} · {a.accountNumber}</p>
                                    </div>
                                    <p className="text-base font-extrabold text-ink">{formatAmount(a.balance)}</p>
                                </Link>
                            </li>
                        ))}
                    </ul>
                )}
            </Card>

            <div className="grid gap-4 lg:grid-cols-2">
                <BalanceChart transactions={recent} />

                {/* Quick actions */}
                <Card className="p-5 sm:p-6">
                    <h2 className="text-lg font-bold">Quick actions</h2>
                    <p className="mt-0.5 text-sm text-slate-500">Move money or open a new account.</p>
                    <div className="mt-5 grid grid-cols-3 gap-3">
                        <Link href="/accounts" className="flex flex-col items-center gap-2 rounded-2xl border border-slate-100 bg-mist px-2 py-5 transition hover:border-brand-200 hover:bg-brand-50">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-600 text-white">
                <ArrowDownLeft className="h-5 w-5" aria-hidden="true" />
              </span>
                            <span className="text-sm font-semibold text-ink">Deposit</span>
                        </Link>
                        <Link href="/accounts" className="flex flex-col items-center gap-2 rounded-2xl border border-slate-100 bg-mist px-2 py-5 transition hover:border-brand-200 hover:bg-brand-50">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-ink text-white">
                <ArrowUpRight className="h-5 w-5" aria-hidden="true" />
              </span>
                            <span className="text-sm font-semibold text-ink">Withdraw</span>
                        </Link>
                        <Link href="/transfer" className="flex flex-col items-center gap-2 rounded-2xl border border-slate-100 bg-mist px-2 py-5 transition hover:border-brand-200 hover:bg-brand-50">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-lime-accent text-ink">
                <ArrowLeftRight className="h-5 w-5" aria-hidden="true" />
              </span>
                            <span className="text-sm font-semibold text-ink">Transfer</span>
                        </Link>
                    </div>
                    <p className="mt-5 rounded-xl bg-slate-50 px-4 py-3 text-sm leading-relaxed text-slate-500">
                        Deposits and withdrawals happen on an account&apos;s detail page. Transfers need the
                        destination account&apos;s ID — you can find an account&apos;s ID on its detail page.
                    </p>
                </Card>
            </div>

            {/* Recent transactions */}
            <Card className="p-5 sm:p-6">
                <div className="flex items-center justify-between">
                    <h2 className="text-lg font-bold">Recent transactions</h2>
                    <Link href="/transactions" className="text-sm font-semibold text-brand-700 hover:underline">
                        View all
                    </Link>
                </div>
                {recent.length === 0 ? (
                    <EmptyState
                        icon={ReceiptText}
                        title="No transactions yet"
                        description="Once you deposit, withdraw or transfer, your latest activity will show up here."
                    />
                ) : (
                    <div className="mt-2">
                        <TransactionTable transactions={recent} />
                    </div>
                )}
            </Card>
        </div>
    )
}
