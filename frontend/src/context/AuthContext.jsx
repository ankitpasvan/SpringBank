'use client'

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api, { TOKEN_KEY } from '@/lib/api'
import { fetchMe, login as loginApi, logout as logoutApi, register as registerApi, updateMe } from '@/services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null)
    const [token, setToken] = useState(null)
    const [loading, setLoading] = useState(true)

    const loadUser = useCallback(async () => {
        try {
            const me = await fetchMe()
            setUser(me)
        } catch {
            setUser(null)
        }
    }, [])

    useEffect(() => {
        async function init() {
            const stored = localStorage.getItem(TOKEN_KEY)
            setToken(stored)
            if (stored) {
                await loadUser()
            }
            setLoading(false)
        }
        init()
    }, [loadUser])

    const login = useCallback(async ({ email, password }) => {
        const data = await loginApi({ email, password })
        setToken(data.token)
        setUser(data.user || null)
        if (!data.user) await loadUser()
        return data
    }, [loadUser])

    // Register returns no token — the caller redirects to /login afterwards.
    const register = useCallback(async ({ name, email, password }) => {
        return registerApi({ name, email, password })
    }, [])

    const updateProfile = useCallback(async ({ name }) => {
        const updated = await updateMe({ name })
        setUser(updated)
        return updated
    }, [])

    const logout = useCallback(() => {
        logoutApi()
        setToken(null)
        setUser(null)
        // Clear the axios default in case it was set; the interceptor reads storage.
        delete api.defaults.headers.common.Authorization
    }, [])

    const value = useMemo(
        () => ({ user, token, loading, login, register, logout, updateProfile, refreshUser: loadUser }),
        [user, token, loading, login, register, logout, updateProfile, loadUser],
    )

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
    const ctx = useContext(AuthContext)
    if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
    return ctx
}
