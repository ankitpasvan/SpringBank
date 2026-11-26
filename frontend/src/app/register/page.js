'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/context/AuthContext.jsx'
import { useToast } from '@/context/ToastContext.jsx'
import { getErrorMessage, getErrorStatus, getFieldErrors } from '@/lib/api'
import { validateEmail, validatePassword, validateRegisterName } from '@/utils/validation'
import Button from '@/components/common/Button.jsx'
import Input from '@/components/common/Input.jsx'
import Alert from '@/components/common/Alert.jsx'
import Card from '@/components/common/Card.jsx'

import PublicLayout from '@/components/layout/PublicLayout.jsx'

function RegisterForm() {
    const { register } = useAuth()
    const toast = useToast()
    const router = useRouter()

    const [form, setForm] = useState({ name: '', email: '', password: '', confirmPassword: '' })
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
        const nameError = validateRegisterName(form.name)
        if (nameError) next.name = nameError
        const emailError = validateEmail(form.email)
        if (emailError) next.email = emailError
        const passwordError = validatePassword(form.password)
        if (passwordError) next.password = passwordError
        if (form.confirmPassword !== form.password) next.confirmPassword = 'Passwords do not match.'
        setErrors(next)
        if (Object.keys(next).length) return

        setSubmitting(true)
        setServerError('')
        try {
            await register({ name: form.name.trim(), email: form.email.trim(), password: form.password })
            // Registration returns no token, so the user signs in on the login page.
            toast.success('Account created. Please log in to continue.')
            router.replace('/login?registered=1')
        } catch (err) {
            const status = getErrorStatus(err)
            if (status === 409) {
                setServerError('An account with this email already exists. Try logging in instead.')
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
            <h1 className="text-3xl font-extrabold tracking-tight">Create your account</h1>
            <p className="mt-2 text-slate-600">
                Already have an account?{' '}
                <Link href="/login" className="font-semibold text-brand-700 hover:underline">
                    Log in
                </Link>
            </p>

            {serverError && (
                <Alert type="error" className="mt-6">
                    {serverError}
                </Alert>
            )}

            <Card className="mt-6 p-6 sm:p-8">
                <form onSubmit={handleSubmit} noValidate className="space-y-5">
                    <Input
                        label="Full name"
                        id="register-name"
                        type="text"
                        autoComplete="name"
                        placeholder="Ankit Pasvan"
                        value={form.name}
                        onChange={setField('name')}
                        error={errors.name}
                        hint="2–50 characters."
                    />
                    <Input
                        label="Email address"
                        id="register-email"
                        type="email"
                        autoComplete="email"
                        placeholder="you@example.com"
                        value={form.email}
                        onChange={setField('email')}
                        error={errors.email}
                    />
                    <Input
                        label="Password"
                        id="register-password"
                        type="password"
                        autoComplete="new-password"
                        placeholder="At least 8 characters"
                        value={form.password}
                        onChange={setField('password')}
                        error={errors.password}
                        hint="8–72 characters, with at least one letter and one digit."
                    />
                    <Input
                        label="Confirm password"
                        id="register-confirm-password"
                        type="password"
                        autoComplete="new-password"
                        placeholder="Repeat your password"
                        value={form.confirmPassword}
                        onChange={(e) => setForm((f) => ({ ...f, confirmPassword: e.target.value }))}
                        error={errors.confirmPassword}
                    />
                    <Button type="submit" loading={submitting} className="w-full" size="lg">
                        Create account
                    </Button>
                </form>
                <p className="mt-5 text-center text-sm text-slate-500">
                    After registering you will log in — registration does not sign you in automatically.
                </p>
            </Card>
        </div>
    )
}


export default function RegisterPage() {
    return (
        <PublicLayout>
            <RegisterForm />
        </PublicLayout>
    )
}