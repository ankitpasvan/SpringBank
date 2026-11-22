'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/context/AuthContext.jsx'
import AppLayout from '@/components/layout/AppLayout.jsx'
import Spinner from '@/components/common/Spinner.jsx'

// Protected area: every page under (app) requires a signed-in session.
// The redirect is client-side via AuthContext — there is no middleware JWT
// verification, because the token lives in localStorage and the backend is
// the authority on every API call.
export default function AppGroupLayout({ children }) {
    const { token, loading } = useAuth()
    const router = useRouter()

    useEffect(() => {
        if (!loading && !token) {
            router.replace('/login')
        }
    }, [loading, token, router])

    if (loading || !token) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-mist">
                <Spinner label="Checking your session…" />
            </div>
        )
    }

    return <AppLayout>{children}</AppLayout>
}
