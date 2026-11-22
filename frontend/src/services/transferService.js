import api from '@/lib/api'

function newIdempotencyKey() {
    return typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `key-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

// POST /api/accounts/{accountId}/transfer (201)
// { toAccountId, amount, description? }
// -> TransferResponse { transactionId, fromAccountId, toAccountId, amount,
//    senderBalanceAfter, description, createdAt }
// CRITICAL: toAccountId is the destination account's UUID id, NOT its
// 12-digit account number. Same-account transfer is rejected by the backend.
export async function transfer(fromAccountId, { toAccountId, amount, description }) {
    const { data } = await api.post(
        `/accounts/${fromAccountId}/transfer`,
        { toAccountId, amount: Number(amount), description: description || undefined },
        { headers: { 'Idempotency-Key': newIdempotencyKey() } },
    )
    return data
}
