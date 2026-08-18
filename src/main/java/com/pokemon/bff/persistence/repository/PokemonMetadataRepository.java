package com.pokemon.bff.persistence.repository;

import com.pokemon.bff.persistence.entity.PokemonMetadataEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PokemonMetadataRepository extends JpaRepository<PokemonMetadataEntity, Long> {
    Optional<PokemonMetadataEntity> findByPokemonId(Integer pokemonId);
}
