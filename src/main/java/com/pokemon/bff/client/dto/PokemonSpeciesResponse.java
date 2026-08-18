package com.pokemon.bff.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PokemonSpeciesResponse(List<Genus> genera,
                                     @JsonProperty("flavor_text_entries") List<FlavorTextEntry> flavorTextEntries,
                                     @JsonProperty("evolution_chain") EvolutionChain evolutionChain) {
    public record Genus(String genus, Language language) {}
    public record FlavorTextEntry(@JsonProperty("flavor_text") String flavorText, Language language) {}
    public record EvolutionChain(String url) {}
    public record Language(String name) {}
}
