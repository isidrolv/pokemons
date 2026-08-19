package com.pokemon.bff.dto;

import java.util.List;

public record PokemonUpdateRequest(
        String name,
        String imageUrl,
        Double height,
        Double weight,
        String description,
        List<PokemonStat> stats,
        List<Skill> skills,
        String localizedName,
        String region,
        String classificationTag
) {
}
