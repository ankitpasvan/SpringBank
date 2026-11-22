import api from '@/lib/api'

// POST /api/accounts (201) { accountType: 'SAVINGS' | 'CURRENT' }
// -> AccountResponse { id, accountNumber, userId, accountType, balance, createdAt, updatedAt }
export async function createAccount(accountType) {
    const { data } = await api.post('/accounts', { accountType })
    return data
}

// GET /api/accounts -> AccountResponse[]
export async function listAccounts() {
    const { data } = await api.get('/accounts')
    return Array.isArray(data) ? data : []
}

// GET /api/accounts/{id} -> AccountResponse (other user's account = 404)
export async function getAccount(id) {
    const { data } = await api.get(`/accounts/${id}`)
    return data
}
