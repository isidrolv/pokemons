import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import SearchBar from './SearchBar'

describe('SearchBar', () => {
  it('forwards input changes and submits trimmed value', () => {
    const onChange = vi.fn()
    const onSubmit = vi.fn()

    const { rerender } = render(<SearchBar value="" onChange={onChange} onSubmit={onSubmit} />)

    fireEvent.change(screen.getByLabelText('Buscar pokemon por nombre o id'), {
      target: { value: '  raichu  ' },
    })
    expect(onChange).toHaveBeenCalledWith('  raichu  ')

    rerender(<SearchBar value="  raichu  " onChange={onChange} onSubmit={onSubmit} />)
    fireEvent.submit(screen.getByRole('button', { name: 'Buscar' }).closest('form'))

    expect(onSubmit).toHaveBeenCalledWith('raichu')
  })
})
