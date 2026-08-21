const BASE_URL = import.meta.env.VITE_AUTH_BASE_URL ?? '/api/auth'

async function postAuth(path, payload, invalidCredentialsMessage) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })

  if (response.status === 400 || response.status === 401) {
    throw new Error(invalidCredentialsMessage)
  }
  if (!response.ok) {
    throw new Error(`No se pudo completar la solicitud (HTTP ${response.status})`)
  }

  return response.json()
}

export async function login(username, password) {
  return postAuth('/login', { username, password }, 'Usuario o contraseña incorrectos.')
}

export async function register(username, email, password) {
  return postAuth(
    '/register',
    { username, email, password },
    'No se pudo crear la cuenta. Verifica los datos o intenta con otro usuario/correo.',
  )
}
