'use client'

import { useState } from 'react'
import Link from 'next/link'
import { Landmark, Menu, X } from 'lucide-react'
import Toast from '../common/Toast.jsx'

export function Logo({ dark = false }) {
    return (
        <Link href="/" className="flex items-center gap-2" aria-label="SpringBank home">
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white">
                <Landmark className="h-5 w-5" aria-hidden="true" />
            </span>
            <span className={`text-xl font-extrabold tracking-tight ${dark ? 'text-white' : 'text-ink'}`}>
                Spring<span className="text-brand-600">Bank</span>
            </span>
        </Link>
    )
}

const NAV_LINKS = [
    { href: '/converter', label: 'Converter' },
    { href: '/pricing', label: 'Pricing' },
    { href: '/business', label: 'Business' },
    { href: '/help', label: 'Help' },
]

export default function PublicLayout({ children }) {
    const [open, setOpen] = useState(false)
    return (
        <div className="flex min-h-screen flex-col bg-white">
            <header className="sticky top-0 z-40 border-b border-slate-100 bg-white/90 backdrop-blur">
                <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
                    <Logo />
                    <nav className="hidden items-center gap-1 md:flex" aria-label="Public">
                        {NAV_LINKS.map((l) => (
                            <Link key={l.href} href={l.href} className="rounded-full px-3 py-2 text-sm font-semibold text-slate-600 transition hover:bg-slate-100 hover:text-ink">{l.label}</Link>
                        ))}
                        <Link href="/login" className="ml-2 rounded-full px-4 py-2 text-sm font-semibold text-ink transition hover:bg-slate-100">Log in</Link>
                        <Link href="/register" className="rounded-full bg-brand-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-brand-700 sm:px-5">Open an account</Link>
                    </nav>
                    <button type="button" onClick={() => setOpen(!open)} className="rounded-full p-2 text-slate-600 hover:bg-slate-100 md:hidden" aria-label="Toggle menu" aria-expanded={open}>
                        {open ? <X className="h-5 w-5" aria-hidden="true" /> : <Menu className="h-5 w-5" aria-hidden="true" />}
                    </button>
                </div>
                {open && (
                    <nav className="border-t border-slate-100 px-4 py-3 md:hidden" aria-label="Mobile">
                        <div className="flex flex-col gap-1">
                            {NAV_LINKS.map((l) => (
                                <Link key={l.href} href={l.href} onClick={() => setOpen(false)} className="rounded-xl px-3 py-2.5 text-sm font-semibold text-ink hover:bg-slate-100">{l.label}</Link>
                            ))}
                            <Link href="/login" onClick={() => setOpen(false)} className="rounded-xl px-3 py-2.5 text-sm font-semibold text-ink hover:bg-slate-100">Log in</Link>
                            <Link href="/register" onClick={() => setOpen(false)} className="mt-1 rounded-full bg-brand-600 px-4 py-2.5 text-center text-sm font-semibold text-white">Open an account</Link>
                        </div>
                    </nav>
                )}
            </header>
            <main className="flex-1">{children}</main>
            <footer className="border-t border-slate-100 bg-mist">
                <div className="mx-auto grid max-w-7xl gap-8 px-4 py-12 sm:grid-cols-4 sm:px-6 lg:px-8">
                    <div>
                        <Logo />
                        <p className="mt-3 max-w-xs text-sm leading-relaxed text-slate-500">A modern, simple way to manage your money — accounts, deposits, withdrawals and transfers, all in one place.</p>
                    </div>
                    <div>
                        <h3 className="text-sm font-bold">Product</h3>
                        <ul className="mt-3 space-y-2 text-sm text-slate-500">
                            <li><Link href="/converter" className="hover:text-brand-700">Currency converter</Link></li>
                            <li><Link href="/pricing" className="hover:text-brand-700">Pricing</Link></li>
                            <li><Link href="/business" className="hover:text-brand-700">Business</Link></li>
                            <li><Link href="/register" className="hover:text-brand-700">Open an account</Link></li>
                        </ul>
                    </div>
                    <div>
                        <h3 className="text-sm font-bold">Account</h3>
                        <ul className="mt-3 space-y-2 text-sm text-slate-500">
                            <li><Link href="/login" className="hover:text-brand-700">Log in</Link></li>
                            <li><Link href="/dashboard" className="hover:text-brand-700">Dashboard</Link></li>
                            <li><Link href="/help" className="hover:text-brand-700">Help</Link></li>
                        </ul>
                    </div>
                    <div>
                        <h3 className="text-sm font-bold">Security</h3>
                        <p className="mt-3 text-sm leading-relaxed text-slate-500">Sign-in is protected by JWT authentication with a 1-hour session. Never share your password or token. SpringBank will never ask for them.</p>
                    </div>
                </div>
                <div className="border-t border-slate-200/70 px-4 py-4 text-center text-xs text-slate-400">
                    © {new Date().getFullYear()} SpringBank. Demo — no real money moves. SpringBank is not a licensed bank.
                </div>
            </footer>
            <Toast />
        </div>
    )
}