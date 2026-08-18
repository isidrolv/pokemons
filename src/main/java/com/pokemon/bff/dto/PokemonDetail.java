package com.pokemon.bff.dto;

import java.util.List;

public record PokemonDetail(int id, String name, String image, double height, double mass,
                            List<PokemonStat> coreStats, String description,
                            EvolutionNode evolutionLineage) {
}
