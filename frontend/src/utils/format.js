// The backend has no currency field, so amounts are formatted as plain
// numbers with exactly 2 decimals — no currency symbol is assumed.
export function formatAmount(value) {
    const n = Number(value)
    if (Number.isNaN(n)) return '0.00'
    return n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatDate(iso) {
    if (!iso) return '—'
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return '—'
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
}

export function formatDateTime(iso) {
    if (!iso) return '—'
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return '—'
    return d.toLocaleString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    })
}

export function maskAccountNumber(accountNumber) {
    const s = String(accountNumber || '')
    if (s.length <= 4) return s
    return `•••• ${s.slice(-4)}`
}

export const TRANSACTION_LABELS = {
    DEPOSIT: 'Deposit',
    WITHDRAWAL: 'Withdrawal',
    TRANSFER_OUT: 'Transfer out',
    TRANSFER_IN: 'Transfer in',
}

export function transactionLabel(type) {
    return TRANSACTION_LABELS[type] || type || 'Transaction'
}

// Outgoing types reduce the balance; incoming types increase it.
export function isOutgoing(type) {
    return type === 'WITHDRAWAL' || type === 'TRANSFER_OUT'
}
