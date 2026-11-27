import Link from 'next/link'
import { FileText, Users, Wallet } from 'lucide-react'
import PublicLayout from '@/components/layout/PublicLayout.jsx'

const FEATURES = [
    { Icon: FileText, title: 'Invoices', text: 'Create and send invoices in any supported currency. Track paid / unpaid status from one place. Demo data only.' },
    { Icon: Users, title: 'Batch payments', text: 'Pay up to 1,000 recipients in one batch — payroll and suppliers, simulated. Upload a CSV in the next batch.' },
    { Icon: Wallet, title: 'Team roles', text: 'Add team members with viewer, sender and approver roles. Approvals are required for large demo transfers.' },
]

export default function BusinessPage() {
    return (
        <PublicLayout>
            <div className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
                <span className="inline-flex items-center gap-2 rounded-full bg-brand-50 px-3.5 py-1.5 text-xs font-bold text-brand-700">SpringBank Business — demo</span>
                <h1 className="mt-5 text-3xl font-extrabold tracking-tight sm:text-5xl">Banking for businesses<br />that work worldwide.</h1>
                <p className="mt-4 max-w-2xl text-lg leading-relaxed text-slate-600">Send, receive and manage money in multiple currencies, invoice customers, and pay your team — all from one SpringBank Business account. Demo only, no real money moves.</p>
                <div className="mt-10 grid gap-5 md:grid-cols-3">
                    {FEATURES.map(({ Icon, title, text }) => (
                        <article key={title} className="rounded-2xl border border-slate-100 bg-white p-6 shadow-card">
                            <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-600"><Icon className="h-5 w-5" aria-hidden="true" /></span>
                            <h3 className="mt-4 text-base font-bold">{title}</h3>
                            <p className="mt-2 text-sm leading-relaxed text-slate-600">{text}</p>
                        </article>
                    ))}
                </div>
                <div className="mt-10 rounded-3xl bg-ink p-8 text-white">
                    <h2 className="text-2xl font-extrabold">One-time setup, no monthly fee (demo)</h2>
                    <p className="mt-2 max-w-xl leading-relaxed text-white/70">Business accounts in this demo are free to open. Real business banking would need KYB verification and a licensed partner.</p>
                    <Link href="/register" className="mt-6 inline-flex items-center gap-2 rounded-full bg-white px-6 py-3 font-semibold text-ink transition hover:bg-slate-100">Open a business account</Link>
                </div>
            </div>
        </PublicLayout>
    )
}