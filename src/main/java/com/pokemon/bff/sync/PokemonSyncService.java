package com.pokemon.bff.sync;

import com.pokemon.bff.dto.PokemonDetail;
import com.pokemon.bff.dto.PokemonItem;
import com.pokemon.bff.persistence.entity.PokemonEntity;
import com.pokemon.bff.persistence.entity.PokemonSkillEntity;
import com.pokemon.bff.persistence.entity.PokemonStatEntity;
import com.pokemon.bff.persistence.repository.PokemonRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PokemonSyncService {

    private final PokemonRepository pokemonRepository;

    public PokemonSyncService(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    @Transactional
    public void syncDetail(PokemonDetail detail) {
        PokemonEntity entity = pokemonRepository.findById(detail.id())
                .orElseGet(PokemonEntity::new);

        entity.setId(detail.id());
        entity.setName(detail.name());
        entity.setImageUrl(detail.image());
        entity.setHeight(detail.height());
        entity.setWeight(detail.mass());
        entity.setDescription(detail.description());
        entity.setSyncedAt(Instant.now());

        entity.getStats().clear();
        if (detail.coreStats() != null) {
            detail.coreStats().forEach(stat ->
                    entity.getStats().add(new PokemonStatEntity(entity, stat.name(), stat.value())));
        }

        pokemonRepository.save(entity);
    }

    @Transactional
    public void syncItem(PokemonItem item) {
        PokemonEntity entity = pokemonRepository.findById(item.id())
                .orElseGet(PokemonEntity::new);

        entity.setId(item.id());
        entity.setName(item.name());
        entity.setImageUrl(item.sprite());
        entity.setWeight(item.mass());
        entity.setSyncedAt(Instant.now());

        entity.getSkills().clear();
        if (item.skills() != null) {
            item.skills().forEach(skill ->
                    entity.getSkills().add(new PokemonSkillEntity(entity, skill.name(), skill.url())));
        }

        pokemonRepository.save(entity);
    }
}
