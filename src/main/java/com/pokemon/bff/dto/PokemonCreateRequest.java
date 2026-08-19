package com.pokemon.bff.dto;

import java.util.List;

public record PokemonCreateRequest(
        Integer id,
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
    /** Backward-compatible constructor for payloads without skills. */
    public PokemonCreateRequest(Integer id, String name, String imageUrl, Double height,
            Double weight, List<PokemonStat> stats, String description,
            String localizedName, String region, String classificationTag) {
        this(id, name, imageUrl, height, weight, description, stats, List.of(),
                localizedName, region, classificationTag);
    }

    public PokemonCreateRequest {
        if (stats == null) {
            stats = List.of();
        }
        if (skills == null) {
            skills = List.of();
        }
    }
    public boolean hasRequiredFields() {
        return id != null
                && id > 0
                && name != null
                && !name.isBlank()
                && height != null
                && height >= 0
                && weight != null
                && weight >= 0;
    }
}
