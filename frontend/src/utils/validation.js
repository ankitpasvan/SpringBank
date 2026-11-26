// Frontend validation mirrors the backend rules (backend is authoritative).
// Amount: 0.01 – 1,000,000.00, max 2 decimals. Description: max 100 chars.

export function validateName(name) {
    if (!name || !name.trim()) return 'Name is required.'
    if (name.trim().length < 2) return 'Name must be at least 2 characters.'
    if (name.trim().length > 100) return 'Name must be at most 100 characters.'
    return ''
}

// Register only: backend RegisterRequest caps name at 50 chars
// (UpdateProfileRequest allows 100, so validateName stays at 100 for profile).
export function validateRegisterName(name) {
    if (!name || !name.trim()) return 'Name is required.'
    if (name.trim().length < 2) return 'Name must be at least 2 characters.'
    if (name.trim().length > 50) return 'Name must be at most 50 characters.'
    return ''
}

export function validateEmail(email) {
    if (!email || !email.trim()) return 'Email is required.'
    if (email.length > 100) return 'Email must be at most 100 characters.'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) return 'Enter a valid email address.'
    return ''
}

export function validatePassword(password) {
    if (!password) return 'Password is required.'
    if (password.length < 8) return 'Password must be at least 8 characters.'
    if (password.length > 72) return 'Password must be at most 72 characters.'
    if (!/[A-Za-z]/.test(password)) return 'Password must contain at least one letter.'
    if (!/\d/.test(password)) return 'Password must contain at least one digit.'
    return ''
}

export function validateAmount(value) {
    if (value === '' || value === null || value === undefined) return 'Amount is required.'
    const n = Number(value)
    if (Number.isNaN(n)) return 'Enter a valid amount.'
    if (!/^\d+(\.\d{1,2})?$/.test(String(value).trim())) return 'Amount can have at most 2 decimal places.'
    if (n < 0.01) return 'Amount must be at least 0.01.'
    if (n > 1000000) return 'Amount must be at most 1,000,000.00.'
    return ''
}

export function validateDescription(description) {
    if (description && description.length > 100) return 'Description must be at most 100 characters.'
    return ''
}

export function validateUuid(value) {
    if (!value || !value.trim()) return 'Destination account ID is required.'
    if (!/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(value.trim())) {
        return 'Enter a valid account ID (UUID format, e.g. 123e4567-e89b-12d3-a456-426614174000).'
    }
    return ''
}