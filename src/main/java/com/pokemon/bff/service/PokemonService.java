package com.pokemon.bff.service;

import com.pokemon.bff.client.PokemonClient;
import com.pokemon.bff.client.dto.EvolutionChainResponse;
import com.pokemon.bff.client.dto.PokemonSpeciesResponse;
import com.pokemon.bff.config.PokemonCacheNames;
import com.pokemon.bff.dto.*;
import com.pokemon.bff.persistence.entity.PokemonEntity;
import com.pokemon.bff.persistence.entity.PokemonMetadataEntity;
import com.pokemon.bff.persistence.entity.PokemonSkillEntity;
import com.pokemon.bff.persistence.entity.PokemonStatEntity;
import com.pokemon.bff.persistence.repository.PokemonRepository;
import com.pokemon.bff.sync.PokemonSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class PokemonService {
    private final PokemonClient client;
    private final PokemonSyncService syncService;
    private final PokemonRepository pokemonRepository;
    private PokemonService self;

    public PokemonService(PokemonClient client, PokemonSyncService syncService, PokemonRepository pokemonRepository) {
        this.client = client;
        this.syncService = syncService;
        this.pokemonRepository = pokemonRepository;
        this.self = this;
    }

    @Autowired
    public void setSelf(@Lazy PokemonService self) {
        this.self = self;
    }

    @Cacheable(cacheNames = PokemonCacheNames.POKEMON_PAGE,
            key = "T(com.pokemon.bff.service.PokemonCacheKeys).page(#page, #size)", sync = true)
    public PokemonPage findPage(int page, int size) {
        var list = client.findAll(page * size, size);
        var items = list.results().stream().map(reference -> map(reference.name())).toList();
        return new PokemonPage(items, page, size, list.count(), (list.count() + size - 1) / size);
    }

    @Transactional
    public PokemonEntity createPokemon(PokemonCreateRequest request) {
        validateCreateRequest(request);

        if (pokemonRepository.existsById(request.id())) {
            throw new IllegalArgumentException("Pokemon with id " + request.id() + " already exists");
        }
        if (pokemonRepository.findByName(normalizeName(request.name())).isPresent()) {
            throw new IllegalArgumentException("Pokemon with name " + request.name() + " already exists");
        }

        PokemonEntity entity = new PokemonEntity(request.id(), normalizeName(request.name()), request.imageUrl(),
                request.height(), request.weight(), request.description(), Instant.now());
        entity.setStats(new ArrayList<>());
        entity.setSkills(new ArrayList<>());

        request.stats().forEach(stat -> entity.getStats().add(new PokemonStatEntity(entity, normalizeName(stat.name()), stat.value())));
        request.skills().forEach(skill -> entity.getSkills().add(new PokemonSkillEntity(entity, normalizeName(skill.name()), skill.url())));

        if (hasMetadata(request)) {
            PokemonMetadataEntity metadata = new PokemonMetadataEntity();
            metadata.setPokemon(entity);
            metadata.setLocalizedName(request.localizedName());
            metadata.setRegion(request.region());
            metadata.setClassificationTag(request.classificationTag());
            entity.setMetadata(metadata);
        }

        return pokemonRepository.save(entity);
    }

    @Transactional
    public PokemonEntity updatePokemon(Integer id, PokemonUpdateRequest request) {
        validateUpdateRequest(id, request);

        PokemonEntity entity = pokemonRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pokemon not found with id " + id));

        if (request.name() != null && !request.name().isBlank()) {
            String normalized = normalizeName(request.name());
            if (!normalized.equalsIgnoreCase(entity.getName()) && pokemonRepository.findByName(normalized).isPresent()) {
                throw new IllegalArgumentException("Pokemon with name " + request.name() + " already exists");
            }
            entity.setName(normalized);
        }

        if (request.imageUrl() != null) {
            entity.setImageUrl(request.imageUrl());
        }
        if (request.height() != null) {
            entity.setHeight(request.height());
        }
        if (request.weight() != null) {
            entity.setWeight(request.weight());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.stats() != null) {
            if (entity.getStats() == null) {
                entity.setStats(new ArrayList<>());
            }
            entity.getStats().clear();
            request.stats().forEach(stat -> entity.getStats().add(new PokemonStatEntity(entity, normalizeName(stat.name()), stat.value())));
        }
        if (request.skills() != null) {
            if (entity.getSkills() == null) {
                entity.setSkills(new ArrayList<>());
            }
            entity.getSkills().clear();
            request.skills().forEach(skill -> entity.getSkills().add(new PokemonSkillEntity(entity, normalizeName(skill.name()), skill.url())));
        }

        if (request.localizedName() != null || request.region() != null || request.classificationTag() != null) {
            if (entity.getMetadata() == null) {
                PokemonMetadataEntity metadata = new PokemonMetadataEntity();
                metadata.setPokemon(entity);
                entity.setMetadata(metadata);
            }
            if (request.localizedName() != null) {
                entity.getMetadata().setLocalizedName(request.localizedName());
            }
            if (request.region() != null) {
                entity.getMetadata().setRegion(request.region());
            }
            if (request.classificationTag() != null) {
                entity.getMetadata().setClassificationTag(request.classificationTag());
            }
        }

        entity.setSyncedAt(Instant.now());
        return pokemonRepository.save(entity);
    }

    @Transactional
    public void deletePokemon(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Pokemon id must be positive");
        }
        PokemonEntity entity = pokemonRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pokemon not found with id " + id));
        pokemonRepository.delete(entity);
    }

    public PokemonDetail findByNameOrId(String pokemon) {
        String normalized = PokemonCacheKeys.pokemon(pokemon);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Pokemon identifier must not be blank");
        }
        var detail = self.fetchDetailFromApi(normalized);
        syncService.syncDetail(detail);
        return detail;
    }

    @Cacheable(cacheNames = PokemonCacheNames.POKEMON_DETAIL,
            key = "T(com.pokemon.bff.service.PokemonCacheKeys).pokemon(#pokemon)", sync = true)
    public PokemonDetail fetchDetailFromApi(String pokemon) {
        var details = client.findByNameOrId(pokemon);
        var species = client.findSpeciesByNameOrId(pokemon);
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

        return new PokemonDetail(details.id(), details.name(), image, details.height() / 10.0,
                details.weight() / 10.0, coreStats, description, lineage);
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

    private void validateCreateRequest(PokemonCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Pokemon payload must not be null");
        }
        if (request.id() == null || request.id() <= 0) {
            throw new IllegalArgumentException("Pokemon id must be positive");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Pokemon name must not be blank");
        }
        request.stats().forEach(stat -> {
            if (stat == null || stat.name() == null || stat.name().isBlank()
                    || stat.value() < 0) {
                throw new IllegalArgumentException("Pokemon stats must have a non-blank name and non-negative value");
            }
        });
        request.skills().forEach(skill -> {
            if (skill == null || skill.name() == null || skill.name().isBlank()) {
                throw new IllegalArgumentException("Pokemon skill names must not be blank");
            }
        });
        validateNonNegative(request.height(), "height");
        validateNonNegative(request.weight(), "weight");
    }

    private void validateUpdateRequest(Integer id, PokemonUpdateRequest request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Pokemon id must be positive");
        }
        if (request == null) {
            throw new IllegalArgumentException("Pokemon payload must not be null");
        }
        if (request.name() != null && request.name().isBlank()) {
            throw new IllegalArgumentException("Pokemon name must not be blank");
        }
        if (request.name() == null && request.imageUrl() == null && request.height() == null
                && request.weight() == null && request.description() == null && request.stats() == null
                && request.skills() == null && request.localizedName() == null
                && request.region() == null && request.classificationTag() == null) {
            throw new IllegalArgumentException("At least one field must be provided");
        }
        if (request.stats() != null) {
            request.stats().forEach(stat -> {
                if (stat == null || stat.name() == null || stat.name().isBlank()
                        || stat.value() < 0) {
                    throw new IllegalArgumentException("Pokemon stats must have a non-blank name and non-negative value");
                }
            });
        }
        if (request.skills() != null) {
            request.skills().forEach(skill -> {
                if (skill == null || skill.name() == null || skill.name().isBlank()) {
                    throw new IllegalArgumentException("Pokemon skill names must not be blank");
                }
            });
        }
        validateNonNegative(request.height(), "height");
        validateNonNegative(request.weight(), "weight");
    }

    private void validateNonNegative(Double value, String fieldName) {
        if (value != null && (!Double.isFinite(value) || value < 0)) {
            throw new IllegalArgumentException("Pokemon " + fieldName + " must be finite and non-negative");
        }
    }

    private boolean hasMetadata(PokemonCreateRequest request) {
        return request.localizedName() != null || request.region() != null || request.classificationTag() != null;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
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
        try {
            return Integer.parseInt(normalizedUrl.substring(slashIndex + 1));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid evolution chain URL: " + url, e);
        }
}
}
