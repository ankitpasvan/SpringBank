import { CURRENCIES } from './currency.js'

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || '/api'

// Fallback: 1 INR = X currency. 1 USD = 92.91 INR (verified 27 Nov 2026).
const FALLBACK_PER_INR = {
    INR: 1, USD: 0.010763, EUR: 0.00925, GBP: 0.00805, AUD: 0.0161, CAD: 0.0148, SGD: 0.0139,
}

let cache = { key: '', at: 0, data: null }
const CACHE_MS = 60 * 60 * 1000

export async function fetchRates(base = 'INR', symbols = null) {
    const list = symbols || CURRENCIES.map((c) => c.code).filter((c) => c !== base)
    const key = `${base}:${list.join(',')}`
    if (cache.key === key && Date.now() - cache.at < CACHE_MS && cache.data) return cache.data
    try {
        const res = await fetch(`${API_BASE}/rates?base=${base}&symbols=${list.join(',')}`, { cache: 'no-store' })
        if (res.ok) {
            const data = await res.json()
            const out = { base: data.base, rates: data.rates, fallback: !!data.fallback, source: data.source || 'SpringBank API' }
            cache = { key, at: Date.now(), data: out }
            return out
        }
    } catch { /* fall through to direct Frankfurter, then static fallback */ }
    try {
        const res = await fetch(`https://api.frankfurter.dev/v1/latest?base=${base}&symbols=${list.join(',')}`)
        if (res.ok) {
            const data = await res.json()
            const out = { base, rates: data.rates, fallback: false, source: 'Frankfurter / ECB' }
            cache = { key, at: Date.now(), data: out }
            return out
        }
    } catch { /* fall through */ }
    const rates = {}
    list.forEach((code) => {
        rates[code] = FALLBACK_PER_INR[code] / FALLBACK_PER_INR[base]
    })
    const out = { base, rates, fallback: true, source: 'Demo fallback (indicative)' }
    cache = { key, at: Date.now(), data: out }
    return out
}

export async function fetchRate(from, to) {
    if (from === to) return { rate: 1, fallback: false, source: '—' }
    const data = await fetchRates(from, [to])
    return { rate: Number(data.rates[to] || 0), fallback: data.fallback, source: data.source }
}