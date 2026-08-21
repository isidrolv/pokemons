import { useCallback, useEffect, useState } from 'react'
import SearchBar from './components/SearchBar'
import PokemonCard from './components/PokemonCard'
import Pagination from './components/Pagination'
import LoginDialog from './components/LoginDialog'
import { fetchPokemonByName, fetchPokemonPage } from './api/pokemonApi'
import { useAuth } from './context/AuthContext'
import './App.css'

const DEFAULT_SIZE = 20
const MIN_CHARS_FOR_LIVE_FILTER = 1

function App() {
  const { isAuthenticated, username, logout } = useAuth()
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(DEFAULT_SIZE)
  const [pageData, setPageData] = useState({ items: [], totalPages: 0, totalElements: 0 })
  const [query, setQuery] = useState('')
  const [liveFilterItems, setLiveFilterItems] = useState(null)
  const [liveFilterLoading, setLiveFilterLoading] = useState(false)
  const [searchResult, setSearchResult] = useState(null)
  const [searchTerm, setSearchTerm] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const loadPage = useCallback(async (targetPage, targetSize) => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchPokemonPage(targetPage, targetSize)
      setPageData(data)
      return data
    } catch (err) {
      setError(err.message)
      return null
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (isAuthenticated && searchTerm === null) {
      loadPage(page, size)
    }
  }, [isAuthenticated, page, size, searchTerm, loadPage])

  // Filtrado incremental: a partir de 1 caracter, filtra sobre lo que ya
  // está en memoria (la página actual); si aún no hay nada cargado, lo pide al API primero.
  useEffect(() => {
    if (searchTerm !== null) return

    const trimmed = query.trim()
    if (trimmed.length < MIN_CHARS_FOR_LIVE_FILTER) {
      setLiveFilterItems(null)
      return
    }

    let cancelled = false

    async function runLiveFilter() {
      setLiveFilterLoading(true)
      setError(null)
      try {
        let source = pageData.items
        if (source.length === 0) {
          const data = await fetchPokemonPage(page, size)
          if (cancelled) return
          setPageData(data)
          source = data.items
        }
        const lower = trimmed.toLowerCase()
        const filtered = source.filter((item) => item.name.toLowerCase().includes(lower))
        if (!cancelled) setLiveFilterItems(filtered)
      } catch (err) {
        if (!cancelled) setError(err.message)
      } finally {
        if (!cancelled) setLiveFilterLoading(false)
      }
    }

    runLiveFilter()
    return () => {
      cancelled = true
    }
  }, [query, searchTerm, pageData.items, page, size])

  function handleQueryChange(nextValue) {
    setQuery(nextValue)
    if (nextValue.trim() === '' && searchTerm !== null) {
      setSearchTerm(null)
      setSearchResult(null)
    }
  }

  async function runExactSearch(name) {
    setLoading(true)
    setError(null)
    setSearchTerm(name)
    setLiveFilterItems(null)
    try {
      const result = await fetchPokemonByName(name)
      setSearchResult(result)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  function handleSubmit(trimmedValue) {
    if (trimmedValue) {
      runExactSearch(trimmedValue)
    } else {
      setSearchTerm(null)
      setSearchResult(null)
      setError(null)
    }
  }

  function handlePageChange(nextPage) {
    if (nextPage >= 0 && nextPage < pageData.totalPages) {
      setPage(nextPage)
    }
  }

  function handleSizeChange(nextSize) {
    setSize(nextSize)
    setPage(0)
  }

  const isSearching = searchTerm !== null
  const isLiveFiltering = !isSearching && liveFilterItems !== null
  const cardsToShow = isSearching
    ? (searchResult ? [searchResult] : [])
    : isLiveFiltering
      ? liveFilterItems
      : pageData.items
  const isLoading = loading || liveFilterLoading

  if (!isAuthenticated) {
    return <LoginDialog />
  }

  return (
    <div className="app">
      <header className="app__header">
        <button className="app__logout" type="button" onClick={logout}>
          Cerrar sesión ({username})
        </button>
        <h1 className="app__title">Pokedex</h1>
        <p className="app__subtitle">Busca tu pokemon favorito por nombre o id</p>
        <SearchBar value={query} onChange={handleQueryChange} onSubmit={handleSubmit} />
      </header>
      {!isSearching && !isLiveFiltering && !isLoading && !error && pageData.totalPages > 0 && (
          <Pagination
              page={page}
              size={size}
              totalPages={pageData.totalPages}
              totalElements={pageData.totalElements}
              onPageChange={handlePageChange}
              onSizeChange={handleSizeChange}
          />
      )}

      <main className="app__main">
        {isLoading && <p className="app__status">Cargando...</p>}
        {!isLoading && error && <p className="app__status app__status--error">{error}</p>}
        {!isLoading && !error && isSearching && !searchResult && (
          <p className="app__status">No se encontró ningún pokemon llamado "{searchTerm}".</p>
        )}
        {!isLoading && !error && isLiveFiltering && liveFilterItems.length === 0 && (
          <p className="app__status">Sin coincidencias para "{query.trim()}".</p>
        )}
        {!isLoading && !error && cardsToShow.length > 0 && (
          <div className="app__grid">
            {cardsToShow.map((pokemon) => (
              <PokemonCard key={pokemon.id} pokemon={pokemon} />
            ))}
          </div>
        )}
      </main>

      {!isSearching && !isLiveFiltering && !isLoading && !error && pageData.totalPages > 0 && (
        <Pagination
          page={page}
          size={size}
          totalPages={pageData.totalPages}
          totalElements={pageData.totalElements}
          onPageChange={handlePageChange}
          onSizeChange={handleSizeChange}
        />
      )}
    </div>
  )
}

export default App
