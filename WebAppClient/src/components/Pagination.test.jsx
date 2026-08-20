import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import Pagination from './Pagination'

describe('Pagination', () => {
  it('renders status and forwards navigation and size change', () => {
    const onPageChange = vi.fn()
    const onSizeChange = vi.fn()

    render(
      <Pagination
        page={1}
        size={20}
        totalPages={3}
        totalElements={42}
        onPageChange={onPageChange}
        onSizeChange={onSizeChange}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: '← Anterior' }))
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente →' }))
    fireEvent.change(screen.getByLabelText('Mostrar por página:'), { target: { value: '50' } })

    expect(screen.getByText('Página 2 de 3 · 42 pokemones')).toBeInTheDocument()
    expect(onPageChange).toHaveBeenNthCalledWith(1, 0)
    expect(onPageChange).toHaveBeenNthCalledWith(2, 2)
    expect(onSizeChange).toHaveBeenCalledWith(50)
  })

  it('disables previous and next button on edges', () => {
    const onPageChange = vi.fn()
    const onSizeChange = vi.fn()

    const { rerender } = render(
      <Pagination
        page={0}
        size={20}
        totalPages={3}
        totalElements={42}
        onPageChange={onPageChange}
        onSizeChange={onSizeChange}
      />,
    )

    expect(screen.getByRole('button', { name: '← Anterior' })).toBeDisabled()

    rerender(
      <Pagination
        page={2}
        size={20}
        totalPages={3}
        totalElements={42}
        onPageChange={onPageChange}
        onSizeChange={onSizeChange}
      />,
    )

    expect(screen.getByRole('button', { name: 'Siguiente →' })).toBeDisabled()
  })
})
