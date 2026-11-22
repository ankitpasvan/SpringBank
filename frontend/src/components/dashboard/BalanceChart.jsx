'use client'

import {
    Bar,
    BarChart,
    CartesianGrid,
    Cell,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from 'recharts'
import Card from '@/components/common/Card.jsx'
import EmptyState from '@/components/common/EmptyState.jsx'
import { formatAmount, transactionLabel } from '@/utils/format'
import { BarChart3 } from 'lucide-react'

// Bar chart of money in vs money out, built only from real transaction history.
export default function BalanceChart({ transactions, title = 'Money in vs money out' }) {
    const totals = { DEPOSIT: 0, TRANSFER_IN: 0, WITHDRAWAL: 0, TRANSFER_OUT: 0 }
    let hasData = false
    for (const tx of transactions || []) {
        if (totals[tx.type] !== undefined) {
            totals[tx.type] += Number(tx.amount) || 0
            hasData = true
        }
    }

    const data = [
        { name: 'Deposits', value: totals.DEPOSIT + totals.TRANSFER_IN, kind: 'in' },
        { name: 'Withdrawals', value: totals.WITHDRAWAL + totals.TRANSFER_OUT, kind: 'out' },
    ]

    return (
        <Card className="p-5 sm:p-6">
            <h3 className="text-base font-bold">{title}</h3>
            <p className="mt-0.5 text-sm text-slate-500">Based on your actual transaction history.</p>
            {!hasData ? (
                <EmptyState
                    icon={BarChart3}
                    title="No activity to chart yet"
                    description="Your chart will appear here once you make a deposit, withdrawal or transfer."
                />
            ) : (
                <div className="mt-5 h-64 w-full">
                    <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }} barCategoryGap="30%">
                            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e7eb" />
                            <XAxis dataKey="name" tick={{ fontSize: 13, fill: '#64748b' }} axisLine={false} tickLine={false} />
                            <YAxis
                                tick={{ fontSize: 12, fill: '#94a3b8' }}
                                axisLine={false}
                                tickLine={false}
                                tickFormatter={(v) => formatAmount(v)}
                                width={80}
                            />
                            <Tooltip
                                formatter={(value) => [formatAmount(value), 'Total']}
                                labelFormatter={(label) => `${label}`}
                                cursor={{ fill: '#f1f5f9' }}
                            />
                            <Bar dataKey="value" radius={[10, 10, 0, 0]} maxBarSize={72}>
                                <Cell fill="#16a34a" />
                                <Cell fill="#163300" />
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            )}
            {hasData && (
                <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                    <div className="rounded-xl bg-brand-50 px-4 py-3">
                        <p className="text-xs font-semibold text-slate-500">Money in</p>
                        <p className="mt-0.5 font-bold text-brand-700">{formatAmount(data[0].value)}</p>
                    </div>
                    <div className="rounded-xl bg-slate-100 px-4 py-3">
                        <p className="text-xs font-semibold text-slate-500">Money out</p>
                        <p className="mt-0.5 font-bold text-ink">{formatAmount(data[1].value)}</p>
                    </div>
                </div>
            )}
        </Card>
    )
}

export { transactionLabel }
