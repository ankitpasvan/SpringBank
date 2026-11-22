'use client'

import { ArrowDownLeft, ArrowUpRight } from 'lucide-react'
import { formatAmount, formatDateTime, isOutgoing, transactionLabel } from '@/utils/format'

// Shared transaction list: desktop table + mobile cards, driven only by API data.
export default function TransactionTable({ transactions }) {
    if (!transactions?.length) return null

    return (
        <div>
            {/* Desktop table */}
            <div className="scrollbar-thin hidden overflow-x-auto md:block">
                <table className="w-full min-w-[560px] text-left text-sm">
                    <thead>
                    <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">
                        <th className="py-3 pr-4 font-semibold">Transaction</th>
                        <th className="py-3 pr-4 font-semibold">Date</th>
                        <th className="py-3 pr-4 font-semibold">Description</th>
                        <th className="py-3 text-right font-semibold">Amount</th>
                    </tr>
                    </thead>
                    <tbody>
                    {transactions.map((tx) => {
                        const outgoing = isOutgoing(tx.type)
                        return (
                            <tr key={tx.id} className="border-b border-slate-50 last:border-0 hover:bg-slate-50/60">
                                <td className="py-3.5 pr-4">
                    <span className="flex items-center gap-2.5 font-medium text-slate-800">
                      <span
                          className={`flex h-8 w-8 items-center justify-center rounded-full ${
                              outgoing ? 'bg-red-50 text-red-500' : 'bg-green-50 text-green-600'
                          }`}
                      >
                        {outgoing ? (
                            <ArrowUpRight className="h-4 w-4" aria-hidden="true" />
                        ) : (
                            <ArrowDownLeft className="h-4 w-4" aria-hidden="true" />
                        )}
                      </span>
                        {transactionLabel(tx.type)}
                    </span>
                                </td>
                                <td className="whitespace-nowrap py-3.5 pr-4 text-slate-500">{formatDateTime(tx.createdAt)}</td>
                                <td className="max-w-[220px] truncate py-3.5 pr-4 text-slate-500">{tx.description || '—'}</td>
                                <td className={`whitespace-nowrap py-3.5 text-right font-semibold ${outgoing ? 'text-slate-800' : 'text-green-700'}`}>
                                    {outgoing ? '−' : '+'} {formatAmount(tx.amount)}
                                </td>
                            </tr>
                        )
                    })}
                    </tbody>
                </table>
            </div>

            {/* Mobile cards */}
            <ul className="divide-y divide-slate-100 md:hidden">
                {transactions.map((tx) => {
                    const outgoing = isOutgoing(tx.type)
                    return (
                        <li key={tx.id} className="flex items-center gap-3 py-3.5">
              <span
                  className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full ${
                      outgoing ? 'bg-red-50 text-red-500' : 'bg-green-50 text-green-600'
                  }`}
              >
                {outgoing ? (
                    <ArrowUpRight className="h-5 w-5" aria-hidden="true" />
                ) : (
                    <ArrowDownLeft className="h-5 w-5" aria-hidden="true" />
                )}
              </span>
                            <div className="min-w-0 flex-1">
                                <p className="truncate text-sm font-semibold text-slate-800">{transactionLabel(tx.type)}</p>
                                <p className="truncate text-xs text-slate-500">
                                    {formatDateTime(tx.createdAt)}{tx.description ? ` · ${tx.description}` : ''}
                                </p>
                            </div>
                            <span className={`shrink-0 text-sm font-bold ${outgoing ? 'text-slate-800' : 'text-green-700'}`}>
                {outgoing ? '−' : '+'} {formatAmount(tx.amount)}
              </span>
                        </li>
                    )
                })}
            </ul>
        </div>
    )
}
