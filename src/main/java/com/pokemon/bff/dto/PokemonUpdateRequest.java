package com.pokemon.bff.dto;

public record PokemonUpdateRequest(String name,
                                  String description,
                                  String imageUrl,
                                  String localizedName,
                                  String region,
                                  String classificationTag) {

    public boolean hasAnyValue() {
        return (name != null && !name.isBlank())
                || (description != null && !description.isBlank())
                || (imageUrl != null && !imageUrl.isBlank())
                || (localizedName != null && !localizedName.isBlank())
                || (region != null && !region.isBlank())
                || (classificationTag != null && !classificationTag.isBlank());
    }
}
