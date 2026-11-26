'use client'

import { useCallback, useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { ChevronLeft, ChevronRight, ReceiptText } from 'lucide-react'
import { listAccounts } from '@/services/accountService.js'
import { listTransactions } from '@/services/transactionService.js'
import { getErrorMessage } from '@/lib/api'
import Card from '@/components/common/Card.jsx'
import Button from '@/components/common/Button.jsx'
import Spinner from '@/components/common/Spinner.jsx'
import Alert from '@/components/common/Alert.jsx'
import EmptyState from '@/components/common/EmptyState.jsx'
import TransactionTable from '@/components/common/TransactionTable.jsx'

import { Suspense } from 'react'

const TYPES = [
    { value: '', label: 'All types' },
    { value: 'DEPOSIT', label: 'Deposits' },
    { value: 'WITHDRAWAL', label: 'Withdrawals' },
    { value: 'TRANSFER_OUT', label: 'Transfers out' },
    { value: 'TRANSFER_IN', label: 'Transfers in' },
]

const PAGE_SIZE = 20

function TransactionsContent() {
    const searchParams = useSearchParams()
    const router = useRouter()
    const [accounts, setAccounts] = useState([])
    const [accountId, setAccountId] = useState(searchParams.get('accountId') || '')
    const [type, setType] = useState('')
    const [from, setFrom] = useState('')
    const [to, setTo] = useState('')
    const [page, setPage] = useState(0)
    const [data, setData] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    const loadAccounts = useCallback(async () => {
        try {
            const list = await listAccounts()
            setAccounts(list)
            if (!accountId && list.length > 0) {
                setAccountId(list[0].id)
            }
        } catch (err) {
            setError(getErrorMessage(err))
            setLoading(false)
        }
    }, []) // eslint-disable-line react-hooks/exhaustive-deps

    useEffect(() => {
        loadAccounts()
    }, [loadAccounts])

    const loadTransactions = useCallback(async () => {
        if (!accountId) {
            setLoading(false)
            return
        }
        setLoading(true)
        setError('')
        try {
            const result = await listTransactions(accountId, { type, from, to, page, size: PAGE_SIZE })
            setData(result)
        } catch (err) {
            setError(getErrorMessage(err))
        } finally {
            setLoading(false)
        }
    }, [accountId, type, from, to, page])

    useEffect(() => {
        loadTransactions()
    }, [loadTransactions])

    const applyFilters = (e) => {
        e.preventDefault()
        setPage(0)
        router.replace(accountId ? `/transactions?accountId=${accountId}` : '/transactions')
        loadTransactions()
    }

    const changeAccount = (id) => {
        setAccountId(id)
        setPage(0)
        router.replace(id ? `/transactions?accountId=${id}` : '/transactions')
    }

    const totalPages = data?.totalPages ?? 0

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-extrabold tracking-tight sm:text-3xl">Transactions</h1>
                <p className="mt-1 text-slate-600">Your full history, newest first. Filter by type or date range.</p>
            </div>

            {error && <Alert type="error">{error}</Alert>}

            <Card className="p-5 sm:p-6">
                <form onSubmit={applyFilters} className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
                    <div>
                        <label htmlFor="tx-account" className="mb-1.5 block text-sm font-semibold text-ink">Account</label>
                        <select
                            id="tx-account"
                            value={accountId}
                            onChange={(e) => changeAccount(e.target.value)}
                            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm focus:border-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-600/20"
                        >
                            {accounts.map((a) => (
                                <option key={a.id} value={a.id}>
                                    {a.accountType} · {a.accountNumber}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label htmlFor="tx-type" className="mb-1.5 block text-sm font-semibold text-ink">Type</label>
                        <select
                            id="tx-type"
                            value={type}
                            onChange={(e) => setType(e.target.value)}
                            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm focus:border-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-600/20"
                        >
                            {TYPES.map((t) => (
                                <option key={t.value} value={t.value}>{t.label}</option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label htmlFor="tx-from" className="mb-1.5 block text-sm font-semibold text-ink">From date</label>
                        <input
                            id="tx-from"
                            type="date"
                            value={from}
                            onChange={(e) => setFrom(e.target.value)}
                            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm focus:border-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-600/20"
                        />
                    </div>
                    <div>
                        <label htmlFor="tx-to" className="mb-1.5 block text-sm font-semibold text-ink">To date</label>
                        <input
                            id="tx-to"
                            type="date"
                            value={to}
                            onChange={(e) => setTo(e.target.value)}
                            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm focus:border-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-600/20"
                        />
                    </div>
                    <div className="flex items-end">
                        <Button type="submit" className="w-full">Apply filters</Button>
                    </div>
                </form>
            </Card>

            <Card className="p-5 sm:p-6">
                {loading ? (
                    <Spinner label="Loading transactions…" />
                ) : accounts.length === 0 ? (
                    <EmptyState
                        icon={ReceiptText}
                        title="No accounts yet"
                        description="Open an account first — your transactions will appear here once you have one."
                    />
                ) : !data || (data.content || []).length === 0 ? (
                    <EmptyState
                        icon={ReceiptText}
                        title="No transactions found"
                        description="Nothing matches these filters yet. Try a different type, a wider date range, or make your first deposit."
                    />
                ) : (
                    <>
                        <p className="mb-2 text-sm text-slate-500">
                            {data.totalElements} transaction{data.totalElements === 1 ? '' : 's'} · page {page + 1} of {totalPages}
                        </p>
                        <TransactionTable transactions={data.content} />
                        {totalPages > 1 && (
                            <div className="mt-5 flex items-center justify-between">
                                <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>
                                    <ChevronLeft className="h-4 w-4" aria-hidden="true" />
                                    Previous
                                </Button>
                                <span className="text-sm font-medium text-slate-500">
                  Page {page + 1} of {totalPages}
                </span>
                                <Button variant="secondary" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                                    Next
                                    <ChevronRight className="h-4 w-4" aria-hidden="true" />
                                </Button>
                            </div>
                        )}
                    </>
                )}
            </Card>
        </div>
    )
}


export default function TransactionsPage() {
    return (
        <Suspense fallback={<Spinner label="Loading transactions…" />}>
            <TransactionsContent />
        </Suspense>
    )
}