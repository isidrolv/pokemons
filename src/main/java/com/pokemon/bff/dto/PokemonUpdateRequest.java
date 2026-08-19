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

    /** Convenience constructor for updates that only use the basic fields. */
    public PokemonUpdateRequest(String name, String description, String imageUrl,
            String localizedName, String region, String classificationTag) {
        this(name, imageUrl, null, null, description, null, null,
                localizedName, region, classificationTag);
    }

    public boolean hasRequiredFields() {
        return (name != null && !name.isBlank())
                || imageUrl != null
                || height != null
                || weight != null
                || description != null
                || stats != null
                || skills != null
                || localizedName != null
                || region != null
                || classificationTag != null;
    }
}
