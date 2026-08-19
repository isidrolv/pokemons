package com.pokemon.bff.service;

import com.pokemon.bff.client.PokemonClient;
import com.pokemon.bff.client.dto.EvolutionChainResponse;
import com.pokemon.bff.client.dto.PokemonDetailsResponse;
import com.pokemon.bff.client.dto.PokemonListResponse;
import com.pokemon.bff.client.dto.PokemonReference;
import com.pokemon.bff.client.dto.PokemonSpeciesResponse;
import com.pokemon.bff.config.PokemonCacheNames;
import com.pokemon.bff.persistence.repository.PokemonRepository;
import com.pokemon.bff.sync.PokemonSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PokemonServiceCachingTest {

    @Test
    void shouldCacheDetailLookupsUsingNormalizedPokemonKey() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestCachingConfiguration.class)) {
            PokemonService service = context.getBean(PokemonService.class);
            PokemonClient client = context.getBean(PokemonClient.class);
            PokemonSyncService syncService = context.getBean(PokemonSyncService.class);

            when(client.findByNameOrId("pikachu")).thenReturn(new PokemonDetailsResponse(
                    25, "pikachu", 4, 60, new PokemonDetailsResponse.Sprites("img"),
                    List.of(), List.of()));
            when(client.findSpeciesByNameOrId("pikachu")).thenReturn(new PokemonSpeciesResponse(
                    List.of(),
                    List.of(new PokemonSpeciesResponse.FlavorTextEntry("Electric mouse",
                            new PokemonSpeciesResponse.Language("en"))),
                    new PokemonSpeciesResponse.EvolutionChain("https://pokeapi.co/api/v2/evolution-chain/10/")));
            when(client.findEvolutionChainById(10)).thenReturn(new EvolutionChainResponse(
                    new EvolutionChainResponse.ChainLink(new PokemonReference("pikachu", "u"), List.of())));

            var first = service.findByNameOrId(" Pikachu ");
            var second = service.findByNameOrId("pikachu");

            assertEquals(first, second);
            verify(client, times(1)).findByNameOrId("pikachu");
            verify(client, times(1)).findSpeciesByNameOrId("pikachu");
            verify(client, times(1)).findEvolutionChainById(10);
            verify(syncService, times(1)).syncDetail(any());
        }
    }

    @Test
    void shouldCachePaginatedResponses() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestCachingConfiguration.class)) {
            PokemonService service = context.getBean(PokemonService.class);
            PokemonClient client = context.getBean(PokemonClient.class);
            PokemonSyncService syncService = context.getBean(PokemonSyncService.class);

            when(client.findAll(0, 1)).thenReturn(new PokemonListResponse(1,
                    List.of(new PokemonReference("bulbasaur", "u1"))));
            when(client.findByNameOrId("bulbasaur")).thenReturn(new PokemonDetailsResponse(
                    1, "bulbasaur", 7, 69, new PokemonDetailsResponse.Sprites("img-1"),
                    List.of(new PokemonDetailsResponse.AbilitySlot(
                            new PokemonDetailsResponse.Ability("overgrow", "u-ability"))),
                    List.of()));
            when(client.findSpeciesByNameOrId("bulbasaur")).thenReturn(new PokemonSpeciesResponse(
                    List.of(new PokemonSpeciesResponse.Genus("Seed Pokemon",
                            new PokemonSpeciesResponse.Language("en"))),
                    List.of(),
                    new PokemonSpeciesResponse.EvolutionChain("https://pokeapi.co/api/v2/evolution-chain/1/")));

            var first = service.findPage(0, 1);
            var second = service.findPage(0, 1);

            assertEquals(first, second);
            assertEquals(1, second.content().size());
            verify(client, times(1)).findAll(0, 1);
            verify(client, times(1)).findByNameOrId("bulbasaur");
            verify(client, times(1)).findSpeciesByNameOrId("bulbasaur");
            verify(syncService, times(1)).syncItem(any());
        }
    }

    @Configuration
    @EnableCaching
    static class TestCachingConfiguration {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(PokemonCacheNames.POKEMON_PAGE, PokemonCacheNames.POKEMON_DETAIL);
        }

        @Bean
        PokemonClient pokemonClient() {
            return mock(PokemonClient.class);
        }

        @Bean
        PokemonSyncService pokemonSyncService() {
            return mock(PokemonSyncService.class);
        }

        @Bean
        PokemonRepository pokemonRepository() {
            return mock(PokemonRepository.class);
        }

        @Bean
        PokemonService pokemonService(PokemonClient pokemonClient, PokemonSyncService pokemonSyncService,
                PokemonRepository pokemonRepository) {
            return new PokemonService(pokemonClient, pokemonSyncService, pokemonRepository);
        }
    }
}
