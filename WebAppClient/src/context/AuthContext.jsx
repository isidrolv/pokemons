import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { login as loginRequest, register as registerRequest } from '../api/authApi'
import { setAuthToken, setUnauthorizedHandler } from '../api/pokemonApi'

const STORAGE_KEY = 'pokedex.auth'
const AuthContext = createContext(null)

function readStoredSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function storeSession(session) {
  try {
    if (session) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  } catch {
    // localStorage no disponible (modo privado, etc.); la sesión sigue viva en memoria.
  }
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readStoredSession)

  useEffect(() => {
    setAuthToken(session?.token ?? null)
  }, [session])

  const logout = useCallback(() => {
    setSession(null)
    storeSession(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
  }, [logout])

  const login = useCallback(async (username, password) => {
    const result = await loginRequest(username, password)
    setSession(result)
    storeSession(result)
  }, [])

  const register = useCallback(async (username, email, password) => {
    const result = await registerRequest(username, email, password)
    setSession(result)
    storeSession(result)
  }, [])

  const value = {
    username: session?.username ?? null,
    isAuthenticated: Boolean(session?.token),
    login,
    register,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
