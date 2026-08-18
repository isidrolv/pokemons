package com.pokemon.bff.service;

import com.pokemon.bff.client.PokemonClient;
import com.pokemon.bff.client.dto.EvolutionChainResponse;
import com.pokemon.bff.client.dto.PokemonSpeciesResponse;
import com.pokemon.bff.dto.*;
import com.pokemon.bff.persistence.entity.PokemonEntity;
import com.pokemon.bff.persistence.entity.PokemonStatEntity;
import com.pokemon.bff.persistence.entity.PokemonMetadataEntity;
import com.pokemon.bff.persistence.repository.PokemonRepository;
import com.pokemon.bff.sync.PokemonSyncService;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class PokemonService {
    private final PokemonClient client;
    private final PokemonSyncService syncService;
    private final PokemonRepository pokemonRepository;

    public PokemonService(PokemonClient client,
                          PokemonSyncService syncService,
                          PokemonRepository pokemonRepository) {
        this.client = client;
        this.syncService = syncService;
        this.pokemonRepository = pokemonRepository;
    }

    public PokemonPage findPage(int page, int size) {
        var list = client.findAll(page * size, size);
        var items = list.results().stream().map(reference -> map(reference.name())).toList();
        return new PokemonPage(items, page, size, list.count(), (list.count() + size - 1) / size);
    }

    public PokemonDetail createPokemon(PokemonCreateRequest request) {
        if (request == null || !request.hasRequiredFields()) {
            throw new IllegalArgumentException("Create payload is missing required fields");
        }
        if (pokemonRepository.existsById(request.id())) {
            throw new IllegalArgumentException("Pokemon id already exists: " + request.id());
        }

        String normalizedName = normalizeRequiredName(request.name());
        if (pokemonRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Pokemon name already exists: " + normalizedName);
        }

        var entity = new PokemonEntity(
                request.id(),
                normalizedName,
                normalizeNullable(request.imageUrl()),
                request.height(),
                request.mass(),
                normalizeNullable(request.description()),
                Instant.now());

        entity.getStats().clear();
        if (request.coreStats() != null) {
            for (PokemonStat stat : request.coreStats()) {
                validateStat(stat);
                entity.getStats().add(new PokemonStatEntity(entity, stat.name().trim(), stat.value()));
            }
        }

        if (hasMetadata(request.localizedName(), request.region(), request.classificationTag())) {
            var metadata = new PokemonMetadataEntity();
            metadata.setPokemon(entity);
            metadata.setLocalizedName(normalizeNullable(request.localizedName()));
            metadata.setRegion(normalizeNullable(request.region()));
            metadata.setClassificationTag(normalizeNullable(request.classificationTag()));
            entity.setMetadata(metadata);
        }

        return toDetail(pokemonRepository.save(entity));
    }

    public PokemonDetail updatePokemon(int id, PokemonUpdateRequest request) {
        if (id < 1) {
            throw new IllegalArgumentException("Pokemon id must be positive");
        }
        if (request == null || !request.hasAnyValue()) {
            throw new IllegalArgumentException("Update payload must contain at least one non-empty field");
        }

        var entity = pokemonRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pokemon not found with id: " + id));

        applyOptionalUpdate(entity, request.name(), "name");
        applyOptionalUpdate(entity, request.description(), "description");
        applyOptionalUpdate(entity, request.imageUrl(), "imageUrl");

        var metadata = entity.getMetadata() == null ? new PokemonMetadataEntity() : entity.getMetadata();
        metadata.setPokemon(entity);
        metadata.setLocalizedName(normalizeNullable(request.localizedName()));
        metadata.setRegion(normalizeNullable(request.region()));
        metadata.setClassificationTag(normalizeNullable(request.classificationTag()));

        entity.setMetadata(metadata);
        entity.setSyncedAt(Instant.now());
        pokemonRepository.save(entity);

        return toDetail(entity);
    }

    public void deletePokemon(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("Pokemon id must be positive");
        }

        var entity = pokemonRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pokemon not found with id: " + id));
        pokemonRepository.delete(entity);
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

    private void applyOptionalUpdate(PokemonEntity entity, String value, String field) {
        if (value == null) {
            return;
        }
        String normalized = value.trim();
        switch (field) {
            case "name" -> {
                if (normalized.isBlank()) {
                    throw new IllegalArgumentException("Pokemon name cannot be blank");
                }
                String normalizedName = normalizeRequiredName(normalized);
                if (!entity.getName().equalsIgnoreCase(normalizedName)
                        && pokemonRepository.existsByNameIgnoreCase(normalizedName)) {
                    throw new IllegalArgumentException("Pokemon name already exists: " + normalizedName);
                }
                entity.setName(normalizedName);
            }
            case "description" -> entity.setDescription(normalized.isBlank() ? null : normalized);
            case "imageUrl" -> entity.setImageUrl(normalized.isBlank() ? null : normalized);
            default -> throw new IllegalArgumentException("Unsupported field: " + field);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeRequiredName(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Pokemon name cannot be blank");
        }
        return normalized.toLowerCase();
    }

    private boolean hasMetadata(String localizedName, String region, String classificationTag) {
        return normalizeNullable(localizedName) != null
                || normalizeNullable(region) != null
                || normalizeNullable(classificationTag) != null;
    }

    private void validateStat(PokemonStat stat) {
        if (stat == null || stat.name() == null || stat.name().isBlank() || stat.value() < 0) {
            throw new IllegalArgumentException("Pokemon stats must have a name and a non-negative value");
        }
    }

    private PokemonDetail toDetail(PokemonEntity entity) {
        var stats = entity.getStats() == null ? List.<PokemonStat>of() : entity.getStats().stream()
                .map(stat -> new PokemonStat(stat.getName(), stat.getValue()))
                .toList();
        return new PokemonDetail(
                entity.getId(),
                entity.getName(),
                entity.getImageUrl(),
                entity.getHeight() == null ? 0.0 : entity.getHeight(),
                entity.getWeight() == null ? 0.0 : entity.getWeight(),
                stats,
                entity.getDescription(),
                new EvolutionNode(entity.getName(), List.of())
        );
    }
}
