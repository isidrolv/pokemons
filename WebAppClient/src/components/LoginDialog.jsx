import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import './LoginDialog.css'

export default function LoginDialog() {
  const { login, register } = useAuth()
  const [mode, setMode] = useState('login')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const isRegister = mode === 'register'

  function toggleMode() {
    setMode(isRegister ? 'login' : 'register')
    setError(null)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      if (isRegister) {
        await register(username.trim(), email.trim(), password)
      } else {
        await login(username.trim(), password)
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-dialog">
      <form className="login-dialog__card" onSubmit={handleSubmit}>
        <h1 className="login-dialog__title">Pokedex</h1>
        <p className="login-dialog__subtitle">
          {isRegister ? 'Crea una cuenta para continuar' : 'Inicia sesión para continuar'}
        </p>

        <label className="login-dialog__label" htmlFor="login-dialog-username">
          Usuario
        </label>
        <input
          id="login-dialog-username"
          className="login-dialog__input"
          type="text"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          autoFocus
          required
        />

        {isRegister && (
          <>
            <label className="login-dialog__label" htmlFor="login-dialog-email">
              Correo electrónico
            </label>
            <input
              id="login-dialog-email"
              className="login-dialog__input"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </>
        )}

        <label className="login-dialog__label" htmlFor="login-dialog-password">
          Contraseña
        </label>
        <input
          id="login-dialog-password"
          className="login-dialog__input"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          minLength={isRegister ? 8 : undefined}
          required
        />

        {error && <p className="login-dialog__error">{error}</p>}

        <button className="login-dialog__submit" type="submit" disabled={loading}>
          {loading ? 'Un momento...' : isRegister ? 'Crear cuenta' : 'Iniciar sesión'}
        </button>

        <button className="login-dialog__toggle" type="button" onClick={toggleMode}>
          {isRegister ? '¿Ya tienes cuenta? Inicia sesión' : '¿No tienes cuenta? Regístrate'}
        </button>
      </form>
    </div>
  )
}
