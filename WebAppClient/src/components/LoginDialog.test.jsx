import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginDialog from './LoginDialog'

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}))

import { useAuth } from '../context/AuthContext'

const login = vi.fn()
const register = vi.fn()

beforeEach(() => {
  vi.clearAllMocks()
  useAuth.mockReturnValue({ login, register })
})

describe('LoginDialog', () => {
  it('submits username and password to login by default', async () => {
    login.mockResolvedValue(undefined)
    render(<LoginDialog />)

    fireEvent.change(screen.getByLabelText('Usuario'), { target: { value: 'ash' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'pikachu123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    await waitFor(() => expect(login).toHaveBeenCalledWith('ash', 'pikachu123'))
    expect(register).not.toHaveBeenCalled()
  })

  it('shows the email field and registers after toggling to create-account mode', async () => {
    register.mockResolvedValue(undefined)
    render(<LoginDialog />)

    fireEvent.click(screen.getByRole('button', { name: '¿No tienes cuenta? Regístrate' }))

    expect(screen.getByLabelText('Correo electrónico')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Usuario'), { target: { value: 'misty' } })
    fireEvent.change(screen.getByLabelText('Correo electrónico'), { target: { value: 'misty@example.com' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'watergym123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    await waitFor(() =>
      expect(register).toHaveBeenCalledWith('misty', 'misty@example.com', 'watergym123'),
    )
  })

  it('shows an error message when login fails', async () => {
    login.mockRejectedValue(new Error('Usuario o contraseña incorrectos.'))
    render(<LoginDialog />)

    fireEvent.change(screen.getByLabelText('Usuario'), { target: { value: 'ash' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'wrong' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    expect(await screen.findByText('Usuario o contraseña incorrectos.')).toBeInTheDocument()
  })
})
