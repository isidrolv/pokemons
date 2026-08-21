import { afterEach, describe, expect, it, vi } from 'vitest'
import { login, register } from './authApi'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('authApi', () => {
  it('logs in and returns the session payload', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({ token: 'jwt-token', username: 'ash' }),
      }),
    )

    const result = await login('ash', 'pikachu123')

    expect(fetch).toHaveBeenCalledWith('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'ash', password: 'pikachu123' }),
    })
    expect(result).toEqual({ token: 'jwt-token', username: 'ash' })
  })

  it('throws a friendly error on invalid login credentials', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 401 }),
    )

    await expect(login('ash', 'wrong')).rejects.toThrow('Usuario o contraseña incorrectos.')
  })

  it('registers a new account and returns the session payload', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        status: 201,
        json: async () => ({ token: 'jwt-token', username: 'misty' }),
      }),
    )

    const result = await register('misty', 'misty@example.com', 'watergym123')

    expect(fetch).toHaveBeenCalledWith('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'misty', email: 'misty@example.com', password: 'watergym123' }),
    })
    expect(result).toEqual({ token: 'jwt-token', username: 'misty' })
  })

  it('throws a friendly error when registration is rejected', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 400 }),
    )

    await expect(register('ash', 'ash@example.com', 'short')).rejects.toThrow(
      'No se pudo crear la cuenta. Verifica los datos o intenta con otro usuario/correo.',
    )
  })

  it('throws a generic error for unexpected server failures', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 500 }),
    )

    await expect(login('ash', 'pikachu123')).rejects.toThrow(
      'No se pudo completar la solicitud (HTTP 500)',
    )
  })
})
