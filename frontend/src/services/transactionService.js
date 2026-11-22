import api from '@/lib/api'

function newIdempotencyKey() {
    return typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `key-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

// POST /api/accounts/{accountId}/deposit (201) { amount, description? }
// -> TransactionResponse { id, accountId, type, amount, balanceAfter, description, createdAt }
export async function deposit(accountId, { amount, description }) {
    const { data } = await api.post(
        `/accounts/${accountId}/deposit`,
        { amount: Number(amount), description: description || undefined },
        { headers: { 'Idempotency-Key': newIdempotencyKey() } },
    )
    return data
}

// POST /api/accounts/{accountId}/withdraw (201) — same body/shape.
// Insufficient funds = 422.
export async function withdraw(accountId, { amount, description }) {
    const { data } = await api.post(
        `/accounts/${accountId}/withdraw`,
        { amount: Number(amount), description: description || undefined },
        { headers: { 'Idempotency-Key': newIdempotencyKey() } },
    )
    return data
}

// GET /api/accounts/{accountId}/transactions?type=&from=&to=&page=&size=
// -> PageResponse { content, page, size, totalElements, totalPages }, newest first.
export async function listTransactions(accountId, params = {}) {
    const clean = {}
    if (params.type) clean.type = params.type
    if (params.from) clean.from = params.from
    if (params.to) clean.to = params.to
    clean.page = params.page ?? 0
    clean.size = params.size ?? 20
    const { data } = await api.get(`/accounts/${accountId}/transactions`, { params: clean })
    return data
}
