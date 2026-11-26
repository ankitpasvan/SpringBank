'use client'

import { useState } from 'react'
import { Info, User } from 'lucide-react'
import { useAuth } from '@/context/AuthContext.jsx'
import { useToast } from '@/context/ToastContext.jsx'
import { getErrorMessage } from '@/lib/api'
import { formatDateTime } from '@/utils/format'
import { validateName } from '@/utils/validation'
import Card from '@/components/common/Card.jsx'
import Button from '@/components/common/Button.jsx'
import Input from '@/components/common/Input.jsx'
import Alert from '@/components/common/Alert.jsx'

export default function ProfilePage() {
    const { user, updateProfile } = useAuth()
    const toast = useToast()
    const [name, setName] = useState(user?.name || '')
    const [editing, setEditing] = useState(false)
    const [error, setError] = useState('')
    const [saving, setSaving] = useState(false)

    const startEdit = () => {
        setName(user?.name || '')
        setError('')
        setEditing(true)
    }

    const handleSave = async (e) => {
        e.preventDefault()
        const nameError = validateName(name)
        if (nameError) {
            setError(nameError)
            return
        }
        setSaving(true)
        try {
            await updateProfile({ name: name.trim() })
            toast.success('Your name was updated.')
            setEditing(false)
        } catch (err) {
            toast.error(getErrorMessage(err))
        } finally {
            setSaving(false)
        }
    }

    return (
        <div className="mx-auto max-w-2xl space-y-6">
            <div>
                <h1 className="text-2xl font-extrabold tracking-tight sm:text-3xl">Profile</h1>
                <p className="mt-1 text-slate-600">Your SpringBank profile details.</p>
            </div>

            <Card className="p-6 sm:p-8">
                <div className="flex items-center gap-4">
          <span className="flex h-16 w-16 items-center justify-center rounded-full bg-brand-600 text-2xl font-extrabold text-white">
            {(user?.name || 'S').charAt(0).toUpperCase()}
          </span>
                    <div>
                        <h2 className="text-xl font-extrabold">{user?.name || '—'}</h2>
                        <p className="text-sm text-slate-500">{user?.email || '—'}</p>
                        {user?.createdAt && (
                            <p className="mt-0.5 text-xs text-slate-400">Member since {formatDateTime(user.createdAt)}</p>
                        )}
                    </div>
                </div>

                <dl className="mt-6 space-y-3 border-t border-slate-100 pt-6 text-sm">
                    <div className="flex justify-between gap-3">
                        <dt className="text-slate-500">Full name</dt>
                        <dd className="font-semibold text-ink">{user?.name || '—'}</dd>
                    </div>
                    <div className="flex justify-between gap-3">
                        <dt className="text-slate-500">Email address</dt>
                        <dd className="font-semibold text-ink">{user?.email || '—'}</dd>
                    </div>
                    <div className="flex justify-between gap-3">
                        <dt className="text-slate-500">User ID</dt>
                        <dd className="break-all font-mono text-xs font-semibold text-ink">{user?.id || '—'}</dd>
                    </div>
                </dl>

                {!editing ? (
                    <Button variant="secondary" onClick={startEdit} className="mt-6">
                        <User className="h-4 w-4" aria-hidden="true" />
                        Edit name
                    </Button>
                ) : (
                    <form onSubmit={handleSave} noValidate className="mt-6 space-y-4 border-t border-slate-100 pt-6">
                        <Input
                            label="Full name"
                            id="profile-name"
                            type="text"
                            value={name}
                            onChange={(e) => {
                                setName(e.target.value)
                                setError('')
                            }}
                            error={error}
                            hint="2–100 characters. Your email address cannot be changed."
                        />
                        <div className="flex gap-2">
                            <Button type="submit" loading={saving}>
                                Save changes
                            </Button>
                            <Button type="button" variant="secondary" onClick={() => setEditing(false)}>
                                Cancel
                            </Button>
                        </div>
                    </form>
                )}
            </Card>

            <Alert type="info">
        <span className="flex items-start gap-2">
          <Info className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          <span>
            <strong>Change password is not available yet.</strong> The current backend has no
            change-password endpoint, so this app does not offer one. It will be added once the
            backend supports it.
          </span>
        </span>
            </Alert>
        </div>
    )
}