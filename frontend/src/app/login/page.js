'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useAuth } from '@/context/AuthContext.jsx'
import { useToast } from '@/context/ToastContext.jsx'
import { getErrorMessage, getErrorStatus, getFieldErrors } from '@/lib/api'
import { validateEmail } from '@/utils/validation'
import Button from '@/components/common/Button.jsx'
import Input from '@/components/common/Input.jsx'
import Alert from '@/components/common/Alert.jsx'
import Card from '@/components/common/Card.jsx'

import { Suspense } from 'react'
import PublicLayout from '@/components/layout/PublicLayout.jsx'
import Spinner from '@/components/common/Spinner.jsx'

function LoginForm() {
    const { login } = useAuth()
    const toast = useToast()
    const router = useRouter()
    const searchParams = useSearchParams()
    const registered = searchParams.get('registered') === '1'
    const sessionExpired = searchParams.get('session') === 'expired'

    const [form, setForm] = useState({ email: '', password: '' })
    const [errors, setErrors] = useState({})
    const [serverError, setServerError] = useState('')
    const [submitting, setSubmitting] = useState(false)

    const setField = (key) => (e) => {
        setForm((f) => ({ ...f, [key]: e.target.value }))
        setErrors((prev) => ({ ...prev, [key]: '' }))
        setServerError('')
    }

    const handleSubmit = async (e) => {
        e.preventDefault()
        const next = {}
        const emailError = validateEmail(form.email)
        if (emailError) next.email = emailError
        if (!form.password) next.password = 'Password is required.'
        setErrors(next)
        if (Object.keys(next).length) return

        setSubmitting(true)
        setServerError('')
        try {
            await login({ email: form.email.trim(), password: form.password })
            toast.success('Welcome back! You are signed in.')
            router.replace('/dashboard')
        } catch (err) {
            const status = getErrorStatus(err)
            if (status === 401) {
                setServerError('Incorrect email or password. Please try again.')
            } else {
                setServerError(getErrorMessage(err))
            }
            const fieldErrors = getFieldErrors(err)
            if (Object.keys(fieldErrors).length) setErrors(fieldErrors)
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="mx-auto flex max-w-md flex-col px-4 py-12 sm:py-16">
            <h1 className="text-3xl font-extrabold tracking-tight">Welcome back</h1>
            <p className="mt-2 text-slate-600">
                Log in to your SpringBank account. Don&apos;t have one yet?{' '}
                <Link href="/register" className="font-semibold text-brand-700 hover:underline">
                    Create one
                </Link>
            </p>

            {registered && (
                <Alert type="success" className="mt-6">
                    Your account was created. Please log in with your email and password.
                </Alert>
            )}
            {sessionExpired && (
                <Alert type="info" className="mt-6">
                    Your session expired (tokens last 1 hour). Please log in again.
                </Alert>
            )}
            {serverError && (
                <Alert type="error" className="mt-6">
                    {serverError}
                </Alert>
            )}

            <Card className="mt-6 p-6 sm:p-8">
                <form onSubmit={handleSubmit} noValidate className="space-y-5">
                    <Input
                        label="Email address"
                        id="login-email"
                        type="email"
                        autoComplete="email"
                        placeholder="you@example.com"
                        value={form.email}
                        onChange={setField('email')}
                        error={errors.email}
                    />
                    <Input
                        label="Password"
                        id="login-password"
                        type="password"
                        autoComplete="current-password"
                        placeholder="Your password"
                        value={form.password}
                        onChange={setField('password')}
                        error={errors.password}
                    />
                    <Button type="submit" loading={submitting} className="w-full" size="lg">
                        Log in
                    </Button>
                </form>
                <p className="mt-5 text-center text-sm text-slate-500">
                    Your session lasts 1 hour. After that you will be asked to log in again.
                </p>
            </Card>
        </div>
    )
}


export default function LoginPage() {
    return (
        <PublicLayout>
            <Suspense fallback={<Spinner label="Loading…" />}>
                <LoginForm />
            </Suspense>
        </PublicLayout>
    )
}
