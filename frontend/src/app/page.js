import Link from 'next/link'
import {
    ArrowDownLeft,
    ArrowLeftRight,
    ArrowUpRight,
    CheckCircle2,
    CreditCard,
    Landmark,
    Lock,
    ShieldCheck,
    Wallet,
} from 'lucide-react'
import PublicLayout from '@/components/layout/PublicLayout.jsx'

const FEATURES = [
    {
        Icon: CreditCard,
        title: 'Accounts that fit you',
        text: 'Open a Savings or Current account in seconds. Your 12-digit account number is generated for you, and your balance starts at zero — ready for your first deposit.',
    },
    {
        Icon: ArrowDownLeft,
        title: 'Deposit & withdraw',
        text: 'Add money or take it out whenever you need it. Every movement is recorded as a transaction with the balance after it, so you always know where you stand.',
    },
    {
        Icon: ArrowLeftRight,
        title: 'Send money instantly',
        text: 'Transfer between SpringBank accounts using the destination account ID. Transfers are protected against double-submission and never overdraw your account.',
    },
    {
        Icon: ReceiptIcon,
        title: 'Full transaction history',
        text: 'Filter by type and date, page through your history, and see every deposit, withdrawal and transfer — newest first.',
    },
]

function ReceiptIcon(props) {
    return <Wallet {...props} />
}

const STEPS = [
    { n: '1', title: 'Create your account', text: 'Register with your name, email and a strong password. No paperwork, no waiting room.' },
    { n: '2', title: 'Open a bank account', text: 'Choose Savings or Current. Your account number is issued instantly.' },
    { n: '3', title: 'Deposit and go', text: 'Add funds, send money, and track every transaction from your dashboard.' },
]

function LandingContent() {
    return (
        <div className="bg-white">
            {/* Hero */}
            <section className="mx-auto max-w-7xl px-4 pb-16 pt-14 sm:px-6 sm:pt-20 lg:px-8">
                <div className="grid items-center gap-12 lg:grid-cols-2">
                    <div>
            <span className="inline-flex items-center gap-2 rounded-full bg-brand-50 px-3.5 py-1.5 text-xs font-bold text-brand-700">
              <span className="h-2 w-2 rounded-full bg-brand-500" aria-hidden="true" />
              Modern banking, built on Spring Boot
            </span>
                        <h1 className="mt-5 text-4xl font-extrabold leading-[1.08] tracking-tight sm:text-5xl lg:text-[3.4rem]">
                            Your money,
                            <br />
                            clearly in <span className="text-brand-600">your hands.</span>
                        </h1>
                        <p className="mt-5 max-w-lg text-lg leading-relaxed text-slate-600">
                            SpringBank is a simple, secure way to manage your money online. Open an account,
                            deposit, withdraw and send money — with a clear record of every transaction.
                        </p>
                        <div className="mt-8 flex flex-wrap gap-3">
                            <Link
                                href="/register"
                                className="inline-flex items-center gap-2 rounded-full bg-brand-600 px-6 py-3 font-semibold text-white transition hover:bg-brand-700"
                            >
                                Open an account
                                <ArrowUpRight className="h-4 w-4" aria-hidden="true" />
                            </Link>
                            <Link
                                href="/login"
                                className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-6 py-3 font-semibold text-ink transition hover:bg-slate-50"
                            >
                                Log in
                            </Link>
                        </div>
                        <ul className="mt-8 flex flex-wrap gap-x-6 gap-y-2 text-sm font-medium text-slate-600">
                            {['No hidden fees to open an account', 'JWT-secured sign-in', 'Live balance after every transaction'].map((t) => (
                                <li key={t} className="flex items-center gap-1.5">
                                    <CheckCircle2 className="h-4 w-4 text-brand-600" aria-hidden="true" />
                                    {t}
                                </li>
                            ))}
                        </ul>
                    </div>

                    {/* Hero visual — a dashboard preview built from real UI shapes, no fake data claims */}
                    <div className="relative">
                        <div className="rounded-3xl border border-slate-100 bg-mist p-5 shadow-lift sm:p-6">
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2.5">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600 text-white">
                    <Landmark className="h-5 w-5" aria-hidden="true" />
                  </span>
                                    <div>
                                        <p className="text-sm font-bold text-ink">SpringBank</p>
                                        <p className="text-xs text-slate-500">Your dashboard</p>
                                    </div>
                                </div>
                                <span className="rounded-full bg-lime-accent/60 px-3 py-1 text-xs font-bold text-ink">Live balance</span>
                            </div>
                            <div className="mt-6 rounded-2xl bg-ink p-5 text-white">
                                <p className="text-xs font-semibold uppercase tracking-wide text-white/60">Total balance</p>
                                <p className="mt-1 text-3xl font-extrabold tracking-tight">0.00</p>
                                <p className="mt-1 text-xs text-white/60">Your real balance appears here after you sign in.</p>
                            </div>
                            <div className="mt-4 grid grid-cols-3 gap-3">
                                {[
                                    { Icon: ArrowDownLeft, label: 'Deposit' },
                                    { Icon: ArrowUpRight, label: 'Withdraw' },
                                    { Icon: ArrowLeftRight, label: 'Transfer' },
                                ].map(({ Icon, label }) => (
                                    <div key={label} className="flex flex-col items-center gap-1.5 rounded-2xl border border-slate-100 bg-white px-2 py-4">
                    <span className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-50 text-brand-600">
                      <Icon className="h-4 w-4" aria-hidden="true" />
                    </span>
                                        <span className="text-xs font-semibold text-ink">{label}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Features */}
            <section className="bg-mist py-16 sm:py-20">
                <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                    <h2 className="max-w-xl text-3xl font-extrabold tracking-tight sm:text-4xl">
                        Everything you need to run your money
                    </h2>
                    <p className="mt-3 max-w-2xl text-lg text-slate-600">
                        Every feature below is powered by the SpringBank backend — nothing is a mock-up.
                    </p>
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
                </div>
            </section>

            {/* How it works */}
            <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
                <h2 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Get started in three steps</h2>
                <div className="mt-10 grid gap-5 md:grid-cols-3">
                    {STEPS.map(({ n, title, text }) => (
                        <article key={n} className="relative rounded-2xl border border-slate-100 bg-white p-6 shadow-card">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-lime-accent text-base font-extrabold text-ink">
                {n}
              </span>
                            <h3 className="mt-4 text-base font-bold">{title}</h3>
                            <p className="mt-2 text-sm leading-relaxed text-slate-600">{text}</p>
                        </article>
                    ))}
                </div>
            </section>

            {/* Trust / security */}
            <section className="bg-ink py-16 text-white sm:py-20">
                <div className="mx-auto grid max-w-7xl gap-10 px-4 sm:px-6 lg:grid-cols-2 lg:px-8">
                    <div>
            <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10">
              <ShieldCheck className="h-6 w-6 text-lime-accent" aria-hidden="true" />
            </span>
                        <h2 className="mt-5 text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
                            Security is built in, not bolted on
                        </h2>
                        <p className="mt-4 max-w-lg leading-relaxed text-white/70">
                            Every request is authenticated with a JWT that expires after one hour. Transfers are
                            protected against duplicate submission, and the backend refuses any withdrawal or
                            transfer that would overdraw your account.
                        </p>
                    </div>
                    <ul className="space-y-4 self-center">
                        {[
                            { Icon: Lock, title: 'JWT authentication', text: 'Your session token is sent securely and expires after 1 hour.' },
                            { Icon: ShieldCheck, title: 'No overdrafts, ever', text: 'Insufficient-funds withdrawals and transfers are rejected by the server.' },
                            { Icon: CheckCircle2, title: 'Duplicate-safe money moves', text: 'Each deposit, withdrawal and transfer carries a unique idempotency key.' },
                        ].map(({ Icon, title, text }) => (
                            <li key={title} className="flex gap-4 rounded-2xl bg-white/5 p-5">
                                <Icon className="h-6 w-6 shrink-0 text-lime-accent" aria-hidden="true" />
                                <div>
                                    <p className="font-bold">{title}</p>
                                    <p className="mt-1 text-sm leading-relaxed text-white/70">{text}</p>
                                </div>
                            </li>
                        ))}
                    </ul>
                </div>
            </section>

            {/* Final CTA */}
            <section className="mx-auto max-w-7xl px-4 py-16 text-center sm:px-6 sm:py-20 lg:px-8">
                <h2 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Ready to take control of your money?</h2>
                <p className="mx-auto mt-3 max-w-xl text-lg text-slate-600">
                    Open your SpringBank account today — it takes less than a minute.
                </p>
                <Link
                    href="/register"
                    className="mt-8 inline-flex items-center gap-2 rounded-full bg-brand-600 px-7 py-3.5 font-semibold text-white transition hover:bg-brand-700"
                >
                    Create your free account
                    <ArrowUpRight className="h-4 w-4" aria-hidden="true" />
                </Link>
            </section>
        </div>
    )
}


export default function LandingPage() {
    return (
        <PublicLayout>
            <LandingContent />
        </PublicLayout>
    )
}
