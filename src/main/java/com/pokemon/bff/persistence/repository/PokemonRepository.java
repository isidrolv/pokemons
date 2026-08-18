package com.pokemon.bff.persistence.repository;

import com.pokemon.bff.persistence.entity.PokemonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PokemonRepository extends JpaRepository<PokemonEntity, Integer> {
    Optional<PokemonEntity> findByName(String name);
    boolean existsByNameIgnoreCase(String name);
}
