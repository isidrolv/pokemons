package com.pokemon.bff.persistence.repository;

import com.pokemon.bff.persistence.entity.PokemonEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PokemonRepository extends JpaRepository<PokemonEntity, Integer> {
    Optional<PokemonEntity> findByName(String name);
}
