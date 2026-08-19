package com.pokemon.bff.dto;

import java.util.List;

public record PokemonPage(List<PokemonItem> content, int page, int size,
                          int totalElements, int totalPages) {
}
