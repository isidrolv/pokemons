package com.pokemon.bff.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PokemonDetailsResponse(int id, String name, int height, int weight, Sprites sprites,
                                     List<AbilitySlot> abilities, List<StatSlot> stats) {
    public record Sprites(@JsonProperty("front_default") String frontDefault) {}
    public record AbilitySlot(Ability ability) {}
    public record Ability(String name, String url) {}
    public record StatSlot(@JsonProperty("base_stat") int baseStat, Stat stat) {}
    public record Stat(String name, String url) {}
}
