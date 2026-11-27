import api from '@/lib/api.js'

export async function listPots() {
    const { data } = await api.get('/pots')
    return data
}

export async function createPot(payload) {
    const { data } = await api.post('/pots', payload)
    return data
}

export async function addToPot(id, accountId, amount) {
    const { data } = await api.post(`/pots/${id}/add`, { accountId, amount: Number(amount) })
    return data
}

export async function withdrawFromPot(id, accountId, amount) {
    const { data } = await api.post(`/pots/${id}/withdraw`, { accountId, amount: Number(amount) })
    return data
}

export async function deletePot(id) {
    await api.delete(`/pots/${id}`)
}