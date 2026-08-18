package com.pokemon.bff.dto;

import java.util.List;

public record PokemonCreateRequest(Integer id,
                                   String name,
                                   String imageUrl,
                                   Double height,
                                   Double mass,
                                   List<PokemonStat> coreStats,
                                   String description,
                                   String localizedName,
                                   String region,
                                   String classificationTag) {

    public boolean hasRequiredFields() {
        return id != null
                && id > 0
                && name != null
                && !name.isBlank()
                && height != null
                && height >= 0
                && mass != null
                && mass >= 0;
    }
}
