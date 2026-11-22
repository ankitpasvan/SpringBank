'use client'

import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'

const ToastContext = createContext(null)

let nextId = 1

export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([])
    const timers = useRef(new Map())

    const push = useCallback((type, message) => {
        const id = nextId++
        setToasts((prev) => [...prev, { id, type, message }])
        const timer = setTimeout(() => {
            setToasts((prev) => prev.filter((t) => t.id !== id))
            timers.current.delete(id)
        }, 5000)
        timers.current.set(id, timer)
    }, [])

    const success = useCallback((message) => push('success', message), [push])
    const error = useCallback((message) => push('error', message), [push])
    const info = useCallback((message) => push('info', message), [push])

    const dismiss = useCallback((id) => {
        clearTimeout(timers.current.get(id))
        timers.current.delete(id)
        setToasts((prev) => prev.filter((t) => t.id !== id))
    }, [])

    const value = useMemo(
        () => ({ toasts, success, error, info, dismiss }),
        [toasts, success, error, info, dismiss],
    )

    return <ToastContext.Provider value={value}>{children}</ToastContext.Provider>
}

export function useToast() {
    const ctx = useContext(ToastContext)
    if (!ctx) throw new Error('useToast must be used inside ToastProvider')
    return ctx
}
