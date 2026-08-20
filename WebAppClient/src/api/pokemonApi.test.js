import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchPokemonByName, fetchPokemonPage } from './pokemonApi'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('pokemonApi', () => {
  it('maps paginated pokemon list response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          content: [
            {
              id: 25,
              name: 'Pikachu',
              sprite: 'pikachu.png',
              category: 'Mouse',
              mass: 6,
              skills: [{ name: 'static' }, { name: 'lightning-rod' }],
            },
          ],
          page: 1,
          size: 20,
          totalElements: 151,
          totalPages: 8,
        }),
      }),
    )

    const result = await fetchPokemonPage(1, 20)

    expect(fetch).toHaveBeenCalledWith('/api/pokemons?page=1&size=20')
    expect(result).toEqual({
      items: [
        {
          id: 25,
          name: 'Pikachu',
          image: 'pikachu.png',
          subtitle: 'Mouse',
          mass: 6,
          tags: ['static', 'lightning-rod'],
        },
      ],
      page: 1,
      size: 20,
      totalElements: 151,
      totalPages: 8,
    })
  })

  it('throws an error when pokemon page cannot be fetched', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
      }),
    )

    await expect(fetchPokemonPage(0, 20)).rejects.toThrow(
      'No se pudo obtener el listado de pokemones (HTTP 500)',
    )
  })

  it('returns null for a not found exact search', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
      }),
    )

    const result = await fetchPokemonByName('missingno')

    expect(result).toBeNull()
  })

  it('maps detail response and truncates description', async () => {
    const description = 'a'.repeat(95)

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({
          id: 1,
          name: 'Bulbasaur',
          image: 'bulbasaur.png',
          description,
          mass: 6.9,
          coreStats: [
            { name: 'hp', value: 45 },
            { name: 'attack', value: 49 },
          ],
        }),
      }),
    )

    const result = await fetchPokemonByName('bulbasaur')

    expect(fetch).toHaveBeenCalledWith('/api/pokemons/bulbasaur')
    expect(result).toEqual({
      id: 1,
      name: 'Bulbasaur',
      image: 'bulbasaur.png',
      subtitle: `${'a'.repeat(90)}…`,
      mass: 6.9,
      tags: ['hp: 45', 'attack: 49'],
    })
  })
})
