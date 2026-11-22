import axios from 'axios'

export const TOKEN_KEY = 'springbank_token'

const api = axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_BASE_URL || '/api',
    headers: { 'Content-Type': 'application/json' },
    timeout: 15000,
})

// Attach JWT on every request when present.
api.interceptors.request.use((config) => {
    const token = typeof window !== 'undefined' ? localStorage.getItem(TOKEN_KEY) : null
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

// 401 = expired / invalid token -> clear session and send to login.
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error?.response?.status === 401) {
            if (typeof window !== 'undefined') {
                localStorage.removeItem(TOKEN_KEY)
                if (window.location.pathname !== '/login') {
                    window.location.href = '/login?session=expired'
                }
            }
        }
        return Promise.reject(error)
    },
)

/**
 * Normalise a backend error into a user-friendly message + per-field errors.
 * Backend error shape: { timestamp, status, error, message, path, fieldErrors }
 */
export function getErrorMessage(error) {
    const data = error?.response?.data
    if (data?.message) return data.message
    if (error?.message) return error.message
    return 'Something went wrong. Please try again.'
}

export function getFieldErrors(error) {
    const data = error?.response?.data
    return data?.fieldErrors && typeof data.fieldErrors === 'object' ? data.fieldErrors : {}
}

export function getErrorStatus(error) {
    return error?.response?.status
}

export default api
