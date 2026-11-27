export const CURRENCIES = [
    { code: 'INR', name: 'Indian Rupee', symbol: '₹' },
    { code: 'USD', name: 'US Dollar', symbol: '$' },
    { code: 'EUR', name: 'Euro', symbol: '€' },
    { code: 'GBP', name: 'British Pound', symbol: '£' },
    { code: 'AUD', name: 'Australian Dollar', symbol: 'A$' },
    { code: 'CAD', name: 'Canadian Dollar', symbol: 'C$' },
    { code: 'SGD', name: 'Singapore Dollar', symbol: 'S$' },
]

export const DEMO_FEE_RATE = 0.0049

export function formatAmount(amount, currency = 'INR') {
    const value = Number(amount || 0)
    try {
        return new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency,
            maximumFractionDigits: 2,
        }).format(value)
    } catch {
        return `${value.toFixed(2)} ${currency}`
    }
}

export function formatNumber(amount) {
    return new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 }).format(Number(amount || 0))
}

export function demoFee(amount) {
    return Math.max(0, Number(amount || 0) * DEMO_FEE_RATE)
}

export function recipientGets(amount, rate) {
    const fee = demoFee(amount)
    return { fee, gets: Math.max(0, (Number(amount || 0) - fee) * Number(rate || 0)) }
}