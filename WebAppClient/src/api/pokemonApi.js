const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/pokemons'

function truncate(text, max) {
  if (!text) return null
  return text.length > max ? `${text.slice(0, max).trim()}…` : text
}

function normalizeListItem(item) {
  return {
    id: item.id,
    name: item.name,
    image: item.sprite,
    subtitle: item.category,
    mass: item.mass,
    tags: (item.skills ?? []).map((skill) => skill.name),
  }
}

function normalizeDetail(detail) {
  return {
    id: detail.id,
    name: detail.name,
    image: detail.image,
    subtitle: truncate(detail.description, 90),
    mass: detail.mass,
    tags: (detail.coreStats ?? []).map((stat) => `${stat.name}: ${stat.value}`),
  }
}

export async function fetchPokemonPage(page, size) {
  const response = await fetch(`${BASE_URL}?page=${page}&size=${size}`)
  if (!response.ok) {
    throw new Error(`No se pudo obtener el listado de pokemones (HTTP ${response.status})`)
  }
  const data = await response.json()
  return {
    items: data.content.map(normalizeListItem),
    page: data.page,
    size: data.size,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
  }
}

export async function fetchPokemonByName(name) {
  const response = await fetch(`${BASE_URL}/${encodeURIComponent(name)}`)
  if (response.status === 404) {
    return null
  }
  if (!response.ok) {
    throw new Error(`No se pudo buscar el pokemon (HTTP ${response.status})`)
  }
  const data = await response.json()
  return normalizeDetail(data)
}
