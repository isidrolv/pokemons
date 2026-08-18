package com.pokemon.bff.sync;

import com.pokemon.bff.dto.*;
import com.pokemon.bff.persistence.entity.PokemonEntity;
import com.pokemon.bff.persistence.repository.PokemonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokemonSyncServiceTest {

    @Mock
    private PokemonRepository pokemonRepository;

    @InjectMocks
    private PokemonSyncService syncService;

    @Test
    void shouldPersistNewPokemonWhenSyncingDetail() {
        when(pokemonRepository.findById(1)).thenReturn(Optional.empty());
        when(pokemonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var detail = new PokemonDetail(1, "bulbasaur", "img.png", 0.7, 6.9,
                List.of(new PokemonStat("hp", 45), new PokemonStat("attack", 49)),
                "A strange seed was planted on its back.",
                new EvolutionNode("bulbasaur", List.of()));

        syncService.syncDetail(detail);

        ArgumentCaptor<PokemonEntity> captor = ArgumentCaptor.forClass(PokemonEntity.class);
        verify(pokemonRepository).save(captor.capture());
        PokemonEntity saved = captor.getValue();

        assertEquals(1, saved.getId());
        assertEquals("bulbasaur", saved.getName());
        assertEquals("img.png", saved.getImageUrl());
        assertEquals(0.7, saved.getHeight());
        assertEquals(6.9, saved.getWeight());
        assertEquals(2, saved.getStats().size());
        assertEquals("hp", saved.getStats().getFirst().getName());
        assertEquals(45, saved.getStats().getFirst().getValue());
    }

    @Test
    void shouldUpdateExistingPokemonWhenSyncingDetail() {
        PokemonEntity existing = new PokemonEntity(1, "bulbasaur", "old.png", 0.7, 6.9, "old desc", java.time.Instant.now());
        when(pokemonRepository.findById(1)).thenReturn(Optional.of(existing));
        when(pokemonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var detail = new PokemonDetail(1, "bulbasaur", "new.png", 0.7, 6.9,
                List.of(new PokemonStat("hp", 45)),
                "Updated description.",
                new EvolutionNode("bulbasaur", List.of()));

        syncService.syncDetail(detail);

        ArgumentCaptor<PokemonEntity> captor = ArgumentCaptor.forClass(PokemonEntity.class);
        verify(pokemonRepository).save(captor.capture());

        assertEquals("new.png", captor.getValue().getImageUrl());
        assertEquals("Updated description.", captor.getValue().getDescription());
    }

    @Test
    void shouldPersistSkillsWhenSyncingItem() {
        when(pokemonRepository.findById(4)).thenReturn(Optional.empty());
        when(pokemonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var item = new PokemonItem(4, "charmander", "img.png", "Lizard Pokemon", 8.5,
                List.of(new Skill("blaze", "https://pokeapi.co/api/v2/ability/66/")));

        syncService.syncItem(item);

        ArgumentCaptor<PokemonEntity> captor = ArgumentCaptor.forClass(PokemonEntity.class);
        verify(pokemonRepository).save(captor.capture());
        PokemonEntity saved = captor.getValue();

        assertEquals(4, saved.getId());
        assertEquals("charmander", saved.getName());
        assertEquals(1, saved.getSkills().size());
        assertEquals("blaze", saved.getSkills().getFirst().getName());
    }

    @Test
    void shouldHandleNullStatsAndSkillsGracefully() {
        when(pokemonRepository.findById(132)).thenReturn(Optional.empty());
        when(pokemonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var detail = new PokemonDetail(132, "ditto", null, 0.3, 4.0,
                null, null, new EvolutionNode("ditto", List.of()));

        syncService.syncDetail(detail);

        ArgumentCaptor<PokemonEntity> captor = ArgumentCaptor.forClass(PokemonEntity.class);
        verify(pokemonRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getStats().size());
    }
}
