'use client'

import Link from 'next/link'
import { Landmark } from 'lucide-react'
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

export default function PublicLayout({ children }) {
    return (
        <div className="flex min-h-screen flex-col bg-white">
            <header className="sticky top-0 z-40 border-b border-slate-100 bg-white/90 backdrop-blur">
                <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
                    <Logo />
                    <nav className="flex items-center gap-2 sm:gap-3" aria-label="Public">
                        <Link
                            href="/login"
                            className="rounded-full px-4 py-2 text-sm font-semibold text-ink transition hover:bg-slate-100"
                        >
                            Log in
                        </Link>
                        <Link
                            href="/register"
                            className="rounded-full bg-brand-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-brand-700 sm:px-5"
                        >
                            Open an account
                        </Link>
                    </nav>
                </div>
            </header>
            <main className="flex-1">
                {children}
            </main>
            <footer className="border-t border-slate-100 bg-mist">
                <div className="mx-auto grid max-w-7xl gap-8 px-4 py-12 sm:grid-cols-3 sm:px-6 lg:px-8">
                    <div>
                        <Logo />
                        <p className="mt-3 max-w-xs text-sm leading-relaxed text-slate-500">
                            A modern, simple way to manage your money — accounts, deposits, withdrawals and
                            transfers, all in one place.
                        </p>
                    </div>
                    <div>
                        <h3 className="text-sm font-bold">Product</h3>
                        <ul className="mt-3 space-y-2 text-sm text-slate-500">
                            <li><Link href="/register" className="hover:text-brand-700">Open an account</Link></li>
                            <li><Link href="/login" className="hover:text-brand-700">Log in</Link></li>
                            <li><Link href="/register" className="hover:text-brand-700">Send money</Link></li>
                        </ul>
                    </div>
                    <div>
                        <h3 className="text-sm font-bold">Security</h3>
                        <p className="mt-3 text-sm leading-relaxed text-slate-500">
                            Sign-in is protected by JWT authentication with a 1-hour session. Never share your
                            password or token with anyone. SpringBank will never ask for them.
                        </p>
                    </div>
                </div>
                <div className="border-t border-slate-200/70 py-4 text-center text-xs text-slate-400">
                    © {new Date().getFullYear()} SpringBank. Built on the SpringBank Spring Boot backend.
                </div>
            </footer>
            <Toast />
        </div>
    )
}
