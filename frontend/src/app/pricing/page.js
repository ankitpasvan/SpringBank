import Link from 'next/link'
import PublicLayout from '@/components/layout/PublicLayout.jsx'

const ROWS = [
    { item: 'Open an account', fee: 'Free', note: 'No monthly fee' },
    { item: 'Hold money in 7 currencies', fee: 'Free', note: 'INR, USD, EUR, GBP, AUD, CAD, SGD' },
    { item: 'Send / convert money', fee: 'From 0.49%', feeNote: 'Demo fee, shown before you confirm' },
    { item: 'Same-currency transfer (demo)', fee: 'Free', note: 'Between SpringBank accounts' },
    { item: 'Receive money (demo)', fee: 'Free', note: 'Local receipt simulation' },
    { item: 'Virtual card (sandbox)', fee: 'Free', note: 'Not a real card' },
    { item: 'Physical card (sandbox)', fee: '₹0 demo', note: 'Simulation only' },
]

export default function PricingPage() {
    return (
        <PublicLayout>
            <div className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
                <h1 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Pricing — no subscription, no hidden fees</h1>
                <p className="mt-3 max-w-2xl text-lg text-slate-600">You only pay the fee shown before you confirm. Demo pricing below — no real money moves.</p>
                <div className="mt-10 overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-card">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-mist text-xs uppercase tracking-wide text-slate-500">
                        <tr><th className="px-5 py-4">Item</th><th className="px-5 py-4">Fee</th><th className="px-5 py-4">Note</th></tr>
                        </thead>
                        <tbody>
                        {ROWS.map((r) => (
                            <tr key={r.item} className="border-t border-slate-100">
                                <td className="px-5 py-4 font-semibold text-ink">{r.item}</td>
                                <td className="px-5 py-4 font-bold text-brand-700">{r.fee}</td>
                                <td className="px-5 py-4 text-slate-600">{r.note || r.feeNote}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
                <p className="mt-4 text-sm text-slate-500">Exchange rate: indicative mid-market rate. The fee is a separate line — the rate is never marked up in this demo.</p>
                <Link href="/register" className="mt-8 inline-flex items-center gap-2 rounded-full bg-brand-600 px-7 py-3.5 font-semibold text-white transition hover:bg-brand-700">Create your free account</Link>
            </div>
        </PublicLayout>
    )
}