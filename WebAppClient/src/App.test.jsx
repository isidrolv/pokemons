import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

vi.mock('./api/pokemonApi', () => ({
  fetchPokemonPage: vi.fn(),
  fetchPokemonByName: vi.fn(),
}))

vi.mock('./context/AuthContext', () => ({
  useAuth: vi.fn(),
}))

import { fetchPokemonByName, fetchPokemonPage } from './api/pokemonApi'
import { useAuth } from './context/AuthContext'

const pagePayload = {
  items: [
    { id: 1, name: 'Bulbasaur', image: null, subtitle: 'Seed', mass: 6.9, tags: ['grass'] },
    { id: 4, name: 'Charmander', image: null, subtitle: 'Lizard', mass: 8.5, tags: ['fire'] },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
}

beforeEach(() => {
  vi.clearAllMocks()
  fetchPokemonPage.mockResolvedValue(pagePayload)
  fetchPokemonByName.mockResolvedValue({
    id: 25,
    name: 'Pikachu',
    image: null,
    subtitle: 'Mouse',
    mass: 6,
    tags: ['electric'],
  })
  useAuth.mockReturnValue({
    isAuthenticated: true,
    username: 'ash',
    logout: vi.fn(),
  })
})

describe('App', () => {
  it('shows the login dialog instead of the dashboard when not authenticated', () => {
    useAuth.mockReturnValue({ isAuthenticated: false, username: null, logout: vi.fn() })

    render(<App />)

    expect(screen.getByRole('heading', { name: 'Pokedex' })).toBeInTheDocument()
    expect(screen.getByLabelText('Usuario')).toBeInTheDocument()
    expect(fetchPokemonPage).not.toHaveBeenCalled()
  })

  it('loads and renders paginated pokemon list', async () => {
    render(<App />)

    expect(await screen.findByText('Bulbasaur')).toBeInTheDocument()
    expect(screen.getByText('Charmander')).toBeInTheDocument()
    expect(screen.getByText('Página 1 de 1 · 2 pokemones')).toBeInTheDocument()
    expect(fetchPokemonPage).toHaveBeenCalledWith(0, 20)
  })

  it('runs exact search on submit and renders only result card', async () => {
    render(<App />)

    await screen.findByText('Bulbasaur')
    fireEvent.change(screen.getByLabelText('Buscar pokemon por nombre o id'), {
      target: { value: 'pikachu' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    await screen.findByText('Pikachu')
    expect(fetchPokemonByName).toHaveBeenCalledWith('pikachu')
    expect(screen.queryByText('Bulbasaur')).not.toBeInTheDocument()
    expect(screen.queryByText('Página 1 de 1 · 2 pokemones')).not.toBeInTheDocument()
  })

  it('shows live filter no-match message when query has no local matches', async () => {
    render(<App />)

    await screen.findByText('Bulbasaur')
    fireEvent.change(screen.getByLabelText('Buscar pokemon por nombre o id'), {
      target: { value: 'xyz' },
    })

    await waitFor(() => {
      expect(screen.getByText('Sin coincidencias para "xyz".')).toBeInTheDocument()
    })
  })
})
