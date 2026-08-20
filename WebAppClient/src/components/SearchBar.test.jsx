import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import SearchBar from './SearchBar'

describe('SearchBar', () => {
  it('forwards input changes and submits trimmed value', () => {
    const onChange = vi.fn()
    const onSubmit = vi.fn()

    render(<SearchBar value="  pikachu  " onChange={onChange} onSubmit={onSubmit} />)

    fireEvent.change(screen.getByLabelText('Buscar pokemon por nombre o id'), {
      target: { value: 'raichu' },
    })
    fireEvent.submit(screen.getByRole('button', { name: 'Buscar' }).closest('form'))

    expect(onChange).toHaveBeenCalledWith('raichu')
    expect(onSubmit).toHaveBeenCalledWith('pikachu')
  })
})
