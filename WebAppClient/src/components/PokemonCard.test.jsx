import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import PokemonCard from './PokemonCard'

describe('PokemonCard', () => {
  it('renders pokemon details and limits tags to four items', () => {
    render(
      <PokemonCard
        pokemon={{
          id: 7,
          name: 'Squirtle',
          image: 'squirtle.png',
          subtitle: 'Tiny Turtle',
          mass: 9,
          tags: ['torrent', 'rain-dish', 'shell', 'water', 'extra-tag'],
        }}
      />,
    )

    expect(screen.getByText('#007')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Squirtle' })).toHaveAttribute('src', 'squirtle.png')
    expect(screen.getByText('Tiny Turtle')).toBeInTheDocument()
    expect(screen.getByText('Peso: 9 kg')).toBeInTheDocument()
    expect(screen.getByText('torrent')).toBeInTheDocument()
    expect(screen.queryByText('extra-tag')).not.toBeInTheDocument()
  })

  it('shows placeholder when image is missing', () => {
    render(
      <PokemonCard
        pokemon={{
          id: 4,
          name: 'Charmander',
          image: null,
          subtitle: null,
          mass: null,
          tags: [],
        }}
      />,
    )

    expect(screen.queryByRole('img', { name: 'Charmander' })).not.toBeInTheDocument()
    expect(screen.getByText('?')).toBeInTheDocument()
  })
})
