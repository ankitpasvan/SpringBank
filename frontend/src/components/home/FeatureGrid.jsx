import { CreditCard, Globe, Lock, ReceiptText } from 'lucide-react'

const FEATURES = [
    { Icon: Globe, title: 'Multi-currency accounts', text: 'Hold INR, USD, EUR, GBP and more in one SpringBank account. Convert at an indicative mid-market rate with a visible fee.' },
    { Icon: CreditCard, title: 'Cards, simulated', text: 'Virtual and physical card views with freeze/unfreeze and spending limits. Sandbox only — no real card is issued.' },
    { Icon: ReceiptText, title: 'Full transaction history', text: 'Filter by type and date, page through your history, and see every deposit, withdrawal and transfer — newest first.' },
    { Icon: Lock, title: 'Secure by default', text: 'JWT sign-in with a 1-hour session, idempotent money moves, and no overdrafts. The backend refuses any move that would overdraw you.' },
]

export default function FeatureGrid() {
    return (
        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {FEATURES.map(({ Icon, title, text }) => (
                <article key={title} className="rounded-2xl border border-slate-100 bg-white p-6 shadow-card">
                    <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                        <Icon className="h-5 w-5" aria-hidden="true" />
                    </span>
                    <h3 className="mt-4 text-base font-bold">{title}</h3>
                    <p className="mt-2 text-sm leading-relaxed text-slate-600">{text}</p>
                </article>
            ))}
        </div>
    )
}