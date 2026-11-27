import api from '@/lib/api.js'

export async function listBeneficiaries() {
    const { data } = await api.get('/beneficiaries')
    return data
}

export async function createBeneficiary(payload) {
    const { data } = await api.post('/beneficiaries', payload)
    return data
}

export async function verifyBeneficiary(id) {
    const { data } = await api.post(`/beneficiaries/${id}/verify`)
    return data
}

export async function deleteBeneficiary(id) {
    await api.delete(`/beneficiaries/${id}`)
}