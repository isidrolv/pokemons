package com.pokemon.bff.dto;

import java.util.List;

public record PokemonItem(int id, String name, String sprite, String category,
                          double mass, List<Skill> skills) {
}
