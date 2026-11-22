import api, { TOKEN_KEY } from '@/lib/api'

// POST /api/auth/register (201) -> UserResponse { id, name, email, createdAt }
// NOTE: register returns NO token. The caller must redirect to /login.
export async function register({ name, email, password }) {
    const { data } = await api.post('/auth/register', { name, email, password })
    return data
}

// POST /api/auth/login -> { token, user: { id, name, email, createdAt } }
export async function login({ email, password }) {
    const { data } = await api.post('/auth/login', { email, password })
    if (data?.token) localStorage.setItem(TOKEN_KEY, data.token)
    return data
}

// GET /api/users/me -> UserResponse
export async function fetchMe() {
    const { data } = await api.get('/users/me')
    return data
}

// PUT /api/users/me { name } -> UserResponse (email cannot change)
export async function updateMe({ name }) {
    const { data } = await api.put('/users/me', { name })
    return data
}

// No logout endpoint exists on the backend — logout is client-side only.
export function logout() {
    localStorage.removeItem(TOKEN_KEY)
}
