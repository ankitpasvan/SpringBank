'use client'

import { useState } from 'react'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import {
    ArrowLeftRight,
    CreditCard,
    LayoutDashboard,
    LogOut,
    Menu,
    ReceiptText,
    User,
    X,
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext.jsx'
import { Logo } from './PublicLayout.jsx'
import Toast from '../common/Toast.jsx'

const NAV = [
    { to: '/dashboard', label: 'Dashboard', Icon: LayoutDashboard },
    { to: '/accounts', label: 'My Accounts', Icon: CreditCard },
    { to: '/transactions', label: 'Transactions', Icon: ReceiptText },
    { to: '/transfer', label: 'Send Money', Icon: ArrowLeftRight },
    { to: '/profile', label: 'Profile', Icon: User },
]

function SidebarContent({ onNavigate }) {
    const { user, logout } = useAuth()
    const router = useRouter()
    const pathname = usePathname()

    const handleLogout = () => {
        logout()
        onNavigate?.()
        router.push('/login')
    }

    return (
        <div className="flex h-full flex-col">
            <div className="flex h-16 items-center px-5">
                <Logo />
            </div>
            <nav className="mt-2 flex-1 space-y-1 px-3" aria-label="Main">
                {NAV.map(({ to, label, Icon }) => {
                    const isActive = pathname === to || (to !== '/dashboard' && pathname.startsWith(to))
                    return (
                        <Link
                            key={to}
                            href={to}
                            onClick={onNavigate}
                            aria-current={isActive ? 'page' : undefined}
                            className={`flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition ${
                                isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100 hover:text-ink'
                            }`}
                        >
                            <Icon className="h-5 w-5" aria-hidden="true" />
                            {label}
                        </Link>
                    )
                })}
            </nav>
            <div className="border-t border-slate-100 p-3">
                <div className="mb-2 flex items-center gap-3 rounded-xl px-2 py-2">
          <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white">
            {(user?.name || 'S').charAt(0).toUpperCase()}
          </span>
                    <div className="min-w-0">
                        <p className="truncate text-sm font-bold text-ink">{user?.name || 'Account holder'}</p>
                        <p className="truncate text-xs text-slate-500">{user?.email || ''}</p>
                    </div>
                </div>
                <button
                    type="button"
                    onClick={handleLogout}
                    className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold text-red-600 transition hover:bg-red-50"
                >
                    <LogOut className="h-5 w-5" aria-hidden="true" />
                    Log out
                </button>
            </div>
        </div>
    )
}

export default function AppLayout({ children }) {
    const [drawerOpen, setDrawerOpen] = useState(false)
    const { user } = useAuth()

    return (
        <div className="flex min-h-screen bg-mist">
            {/* Desktop sidebar */}
            <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-slate-100 bg-white lg:block">
                <SidebarContent />
            </aside>

            {/* Mobile drawer */}
            {drawerOpen && (
                <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Menu">
                    <div className="absolute inset-0 bg-ink/50" onClick={() => setDrawerOpen(false)} />
                    <aside className="absolute inset-y-0 left-0 w-72 max-w-[85vw] bg-white shadow-lift">
                        <button
                            type="button"
                            onClick={() => setDrawerOpen(false)}
                            aria-label="Close menu"
                            className="absolute right-3 top-4 rounded-full p-2 text-slate-500 hover:bg-slate-100"
                        >
                            <X className="h-5 w-5" aria-hidden="true" />
                        </button>
                        <SidebarContent onNavigate={() => setDrawerOpen(false)} />
                    </aside>
                </div>
            )}

            <div className="flex min-w-0 flex-1 flex-col lg:pl-64">
                <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-slate-100 bg-white/90 px-4 backdrop-blur sm:px-6">
                    <button
                        type="button"
                        onClick={() => setDrawerOpen(true)}
                        aria-label="Open menu"
                        className="rounded-full p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
                    >
                        <Menu className="h-5 w-5" aria-hidden="true" />
                    </button>
                    <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-semibold text-ink">
                            {user ? `Welcome back, ${user.name.split(' ')[0]}` : 'SpringBank'}
                        </p>
                        <p className="hidden text-xs text-slate-500 sm:block">Your money, clearly organised.</p>
                    </div>
                    <span className="hidden items-center gap-2 rounded-full bg-brand-50 px-3 py-1.5 text-xs font-semibold text-brand-700 sm:inline-flex">
            <span className="h-2 w-2 rounded-full bg-brand-500" aria-hidden="true" />
            Secure session
          </span>
                </header>
                <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6 lg:px-8">
                    {children}
                </main>
            </div>
            <Toast />
        </div>
    )
}
