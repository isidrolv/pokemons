import './PokemonCard.css'

export default function PokemonCard({ pokemon }) {
  const { id, name, image, subtitle, mass, tags } = pokemon

  return (
    <article className="pokemon-card">
      <span className="pokemon-card__id">#{String(id).padStart(3, '0')}</span>
      <div className="pokemon-card__image-wrapper">
        {image ? (
          <img className="pokemon-card__image" src={image} alt={name} loading="lazy" />
        ) : (
          <div className="pokemon-card__image pokemon-card__image--placeholder">?</div>
        )}
      </div>
      <h3 className="pokemon-card__name">{name}</h3>
      {subtitle && <p className="pokemon-card__subtitle">{subtitle}</p>}
      {typeof mass === 'number' && (
        <p className="pokemon-card__mass">Peso: {mass} kg</p>
      )}
      {tags?.length > 0 && (
        <ul className="pokemon-card__tags">
          {tags.slice(0, 4).map((tag) => (
            <li key={tag} className="pokemon-card__tag">
              {tag}
            </li>
          ))}
        </ul>
      )}
    </article>
  )
}
