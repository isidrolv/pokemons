package com.pokemon.bff.service;

import com.pokemon.bff.client.PokemonClient;
import com.pokemon.bff.client.dto.EvolutionChainResponse;
import com.pokemon.bff.client.dto.PokemonSpeciesResponse;
import com.pokemon.bff.dto.*;
import com.pokemon.bff.sync.PokemonSyncService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PokemonService {
    private final PokemonClient client;
    private final PokemonSyncService syncService;

    public PokemonService(PokemonClient client, PokemonSyncService syncService) {
        this.client = client;
        this.syncService = syncService;
    }

    public PokemonPage findPage(int page, int size) {
        var list = client.findAll(page * size, size);
        var items = list.results().stream().map(reference -> map(reference.name())).toList();
        return new PokemonPage(items, page, size, list.count(), (list.count() + size - 1) / size);
    }

    public PokemonDetail findByNameOrId(String pokemon) {
        String normalized = pokemon == null ? "" : pokemon.trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Pokemon identifier must not be blank");
        }

        var details = client.findByNameOrId(normalized);
        var species = client.findSpeciesByNameOrId(normalized);
        var evolution = client.findEvolutionChainById(extractEvolutionChainId(species));

        var image = details.sprites() == null ? null : details.sprites().frontDefault();
        var coreStats = details.stats() == null ? List.<PokemonStat>of() : details.stats().stream()
                .filter(statSlot -> statSlot != null && statSlot.stat() != null)
                .map(statSlot -> new PokemonStat(statSlot.stat().name(), statSlot.baseStat()))
                .toList();
        var description = findEnglishDescription(species);
        var lineage = evolution == null || evolution.chain() == null
                ? new EvolutionNode(details.name(), List.of())
                : mapEvolutionChain(evolution.chain());

        var detail = new PokemonDetail(details.id(), details.name(), image, details.height() / 10.0,
                details.weight() / 10.0, coreStats, description, lineage);
        syncService.syncDetail(detail);
        return detail;
    }

    private PokemonItem map(String name) {
        var details = client.findByNameOrId(name);
        var species = client.findSpeciesByNameOrId(name);
        String category = findEnglishCategory(species);
        var skills = details.abilities() == null ? List.<Skill>of() : details.abilities().stream()
                .filter(a -> a != null && a.ability() != null)
                .map(a -> new Skill(a.ability().name(), a.ability().url())).toList();
        var item = new PokemonItem(details.id(), details.name(),
                details.sprites() == null ? null : details.sprites().frontDefault(),
                category, details.weight() / 10.0, skills);
        syncService.syncItem(item);
        return item;
    }

    private String findEnglishCategory(PokemonSpeciesResponse species) {
        if (species.genera() == null) {
            return null;
        }
        return species.genera().stream()
                .filter(g -> g.language() != null && "en".equals(g.language().name()))
                .map(PokemonSpeciesResponse.Genus::genus)
                .findFirst()
                .orElse(null);
    }

    private String findEnglishDescription(PokemonSpeciesResponse species) {
        if (species.flavorTextEntries() == null) {
            return null;
        }
        return species.flavorTextEntries().stream()
                .filter(entry -> entry != null
                        && entry.language() != null
                        && "en".equals(entry.language().name())
                        && entry.flavorText() != null
                        && !entry.flavorText().isBlank())
                .map(entry -> entry.flavorText().replace('\n', ' ')
                        .replace('\f', ' ')
                        .replace('\t', ' ')
                        .replaceAll("\\s+", " ")
                        .trim())
                .findFirst()
                .orElse(null);
    }

    private EvolutionNode mapEvolutionChain(EvolutionChainResponse.ChainLink link) {
        var name = link.species() == null ? null : link.species().name();
        var evolvesTo = link.evolvesTo() == null ? List.<EvolutionNode>of() : link.evolvesTo().stream()
                .map(this::mapEvolutionChain)
                .toList();
        return new EvolutionNode(name, evolvesTo);
    }

    private int extractEvolutionChainId(PokemonSpeciesResponse species) {
        if (species.evolutionChain() == null || species.evolutionChain().url() == null
                || species.evolutionChain().url().isBlank()) {
            throw new IllegalStateException("Evolution chain URL is missing");
        }
        String url = species.evolutionChain().url();
        String normalizedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        int slashIndex = normalizedUrl.lastIndexOf('/');
        if (slashIndex < 0 || slashIndex == normalizedUrl.length() - 1) {
            throw new IllegalStateException("Invalid evolution chain URL: " + url);
        }
        return Integer.parseInt(normalizedUrl.substring(slashIndex + 1));
    }
}
