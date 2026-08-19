package com.pokemon.bff.service;

import com.pokemon.bff.client.PokemonClient;
import com.pokemon.bff.client.dto.*;
import com.pokemon.bff.dto.PokemonDetail;
import com.pokemon.bff.dto.PokemonItem;
import com.pokemon.bff.dto.PokemonCreateRequest;
import com.pokemon.bff.dto.PokemonUpdateRequest;
import com.pokemon.bff.dto.PokemonStat;
import com.pokemon.bff.persistence.entity.PokemonEntity;
import com.pokemon.bff.persistence.repository.PokemonRepository;
import com.pokemon.bff.sync.PokemonSyncService;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PokemonServiceTest {
    private static final String POKEMON_BULBASAUR = "bulbasaur";
    private static final String POKEMON_IVYSAUR = "ivysaur";
    private static final String POKEMON_VENUSAUR = "venusaur";

    @Mock
    private PokemonClient client;

    @Mock
    private PokemonSyncService syncService;

    @Mock
    private PokemonRepository pokemonRepository;

    @InjectMocks
    private PokemonService service;

    @Test
    void shouldReturnDetailedViewWithDescriptionStatsAndLineage() {
        Faker faker = new Faker();
        int pokemonId = faker.number().numberBetween(1, 500);
        String imageUrl = faker.internet().url();
        String hpStatName = faker.lorem().word().toLowerCase();
        int hpStatValue = faker.number().numberBetween(30, 150);
        int attackStatValue = faker.number().numberBetween(30, 150);
        int defenseStatValue = faker.number().numberBetween(30, 150);
        String japaneseDescription = faker.lorem().sentence();
        String englishDescriptionRaw = faker.lorem().sentence() + "\n" + faker.lorem().sentence();
        String expectedEnglishDescription = englishDescriptionRaw.replace('\n', ' ').replaceAll("\\s+", " ").trim();

        when(client.findByNameOrId(POKEMON_BULBASAUR)).thenReturn(new PokemonDetailsResponse(
                pokemonId,
                POKEMON_BULBASAUR,
                7,
                69,
                new PokemonDetailsResponse.Sprites(imageUrl),
                List.of(),
                List.of(
                        new PokemonDetailsResponse.StatSlot(hpStatValue, new PokemonDetailsResponse.Stat(hpStatName, faker.internet().url())),
                        new PokemonDetailsResponse.StatSlot(attackStatValue, new PokemonDetailsResponse.Stat("attack", faker.internet().url())),
                        new PokemonDetailsResponse.StatSlot(defenseStatValue, new PokemonDetailsResponse.Stat("defense", faker.internet().url())))));
        when(client.findSpeciesByNameOrId(POKEMON_BULBASAUR)).thenReturn(new PokemonSpeciesResponse(
                List.of(new PokemonSpeciesResponse.Genus("Seed Pokemon",
                        new PokemonSpeciesResponse.Language("en"))),
                List.of(
                        new PokemonSpeciesResponse.FlavorTextEntry(japaneseDescription,
                                new PokemonSpeciesResponse.Language("ja")),
                        new PokemonSpeciesResponse.FlavorTextEntry(englishDescriptionRaw,
                                new PokemonSpeciesResponse.Language("en"))),
                new PokemonSpeciesResponse.EvolutionChain("https://pokeapi.co/api/v2/evolution-chain/1/")));
        when(client.findEvolutionChainById(1)).thenReturn(new EvolutionChainResponse(
                new EvolutionChainResponse.ChainLink(
                        new PokemonReference(POKEMON_BULBASAUR, "u1"),
                        List.of(new EvolutionChainResponse.ChainLink(
                                new PokemonReference(POKEMON_IVYSAUR, "u2"),
                                List.of(new EvolutionChainResponse.ChainLink(
                                        new PokemonReference(POKEMON_VENUSAUR, "u3"),
                                        List.of())))))));

        var detail = service.findByNameOrId("Bulbasaur");

        assertEquals(pokemonId, detail.id());
        assertEquals(POKEMON_BULBASAUR, detail.name());
        assertEquals(imageUrl, detail.image());
        assertEquals(0.7, detail.height());
        assertEquals(6.9, detail.mass());
        assertEquals(3, detail.coreStats().size());
        assertEquals(hpStatName, detail.coreStats().getFirst().name());
        assertEquals(hpStatValue, detail.coreStats().getFirst().value());
        assertEquals(expectedEnglishDescription, detail.description());
        assertNotNull(detail.evolutionLineage());
        assertEquals(POKEMON_BULBASAUR, detail.evolutionLineage().name());
        assertEquals(POKEMON_IVYSAUR, detail.evolutionLineage().evolvesTo().getFirst().name());
        assertEquals(POKEMON_VENUSAUR, detail.evolutionLineage().evolvesTo().getFirst().evolvesTo().getFirst().name());
        verify(syncService).syncDetail(any(PokemonDetail.class));
    }

    @Test
    void shouldBuildPageWithMappedItems() {
        when(client.findAll(0, 2)).thenReturn(new PokemonListResponse(3, List.of(
                new PokemonReference("bulbasaur", "u1"),
                new PokemonReference("charmander", "u2"))));

        when(client.findByNameOrId("bulbasaur")).thenReturn(new PokemonDetailsResponse(
                1, "bulbasaur", 7, 69, new PokemonDetailsResponse.Sprites("img-1"),
                List.of(new PokemonDetailsResponse.AbilitySlot(new PokemonDetailsResponse.Ability("overgrow", "u-ability-1"))),
                null));
        when(client.findSpeciesByNameOrId("bulbasaur")).thenReturn(new PokemonSpeciesResponse(
                List.of(new PokemonSpeciesResponse.Genus("Seed Pokemon", new PokemonSpeciesResponse.Language("en"))),
                List.of(),
                new PokemonSpeciesResponse.EvolutionChain("https://pokeapi.co/api/v2/evolution-chain/1/")));

        when(client.findByNameOrId("charmander")).thenReturn(new PokemonDetailsResponse(
                4, "charmander", 6, 85, new PokemonDetailsResponse.Sprites("img-4"),
                List.of(new PokemonDetailsResponse.AbilitySlot(new PokemonDetailsResponse.Ability("blaze", "u-ability-2"))),
                null));
        when(client.findSpeciesByNameOrId("charmander")).thenReturn(new PokemonSpeciesResponse(
                List.of(new PokemonSpeciesResponse.Genus("Lizard Pokemon", new PokemonSpeciesResponse.Language("en"))),
                List.of(),
                new PokemonSpeciesResponse.EvolutionChain("https://pokeapi.co/api/v2/evolution-chain/2/")));

        var page = service.findPage(0, 2);

        assertEquals(2, page.content().size());
        assertEquals(3, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals("bulbasaur", page.content().getFirst().name());
        assertEquals("Seed Pokemon", page.content().getFirst().category());
        assertEquals(1, page.content().getFirst().skills().size());
        verify(syncService, org.mockito.Mockito.times(2)).syncItem(any(PokemonItem.class));
    }

    @Test
    void shouldReturnNullCategoryAndEmptySkillsWhenDataMissing() {
        when(client.findAll(0, 1)).thenReturn(new PokemonListResponse(1, List.of(new PokemonReference("ditto", "u-ditto"))));
        when(client.findByNameOrId("ditto")).thenReturn(new PokemonDetailsResponse(
                132, "ditto", 3, 40, null, null, null));
        when(client.findSpeciesByNameOrId("ditto")).thenReturn(new PokemonSpeciesResponse(
                null,
                null,
                new PokemonSpeciesResponse.EvolutionChain("https://pokeapi.co/api/v2/evolution-chain/67/")));

        var page = service.findPage(0, 1);

        assertEquals(1, page.content().size());
        assertEquals("ditto", page.content().getFirst().name());
        assertNull(page.content().getFirst().category());
        assertNull(page.content().getFirst().sprite());
        assertEquals(0, page.content().getFirst().skills().size());
    }

    @Test
    void shouldThrowWhenPokemonIdentifierIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.findByNameOrId("   "));
        assertThrows(IllegalArgumentException.class, () -> service.findByNameOrId(null));
    }

    @Test
    void shouldReturnFallbackLineageWhenEvolutionChainIsNull() {
        when(client.findByNameOrId("eevee")).thenReturn(new PokemonDetailsResponse(
                133, "eevee", 3, 65, null, List.of(), null));
        when(client.findSpeciesByNameOrId("eevee")).thenReturn(new PokemonSpeciesResponse(
                List.of(),
                List.of(new PokemonSpeciesResponse.FlavorTextEntry("text",
                        new PokemonSpeciesResponse.Language("en"))),
                new PokemonSpeciesResponse.EvolutionChain("https://pokeapi.co/api/v2/evolution-chain/67/")));
        when(client.findEvolutionChainById(67)).thenReturn(null);

        var detail = service.findByNameOrId("eevee");

        assertEquals("eevee", detail.evolutionLineage().name());
        assertEquals(0, detail.evolutionLineage().evolvesTo().size());
        assertEquals(0, detail.coreStats().size());
    }

    @Test
    void shouldThrowWhenEvolutionChainUrlIsMissing() {
        when(client.findByNameOrId("mew")).thenReturn(new PokemonDetailsResponse(
                151, "mew", 4, 40, null, List.of(), List.of()));
        when(client.findSpeciesByNameOrId("mew")).thenReturn(new PokemonSpeciesResponse(
                List.of(),
                List.of(),
                null));

        assertThrows(IllegalStateException.class, () -> service.findByNameOrId("mew"));
    }

    @Test
    void shouldThrowWhenEvolutionChainUrlIsInvalid() {
        when(client.findByNameOrId("mewtwo")).thenReturn(new PokemonDetailsResponse(
                150, "mewtwo", 20, 1220, null, List.of(), List.of()));
        when(client.findSpeciesByNameOrId("mewtwo")).thenReturn(new PokemonSpeciesResponse(
                List.of(),
                List.of(),
                new PokemonSpeciesResponse.EvolutionChain("invalid-url")));

        assertThrows(IllegalStateException.class, () -> service.findByNameOrId("mewtwo"));
    }

    @Test
    void shouldReturnNullDescriptionWhenNoEnglishFlavorTextFound() {
        when(client.findByNameOrId("snorlax")).thenReturn(new PokemonDetailsResponse(
                143, "snorlax", 21, 4600, null, List.of(), List.of()));
        when(client.findSpeciesByNameOrId("snorlax")).thenReturn(new PokemonSpeciesResponse(
                List.of(),
                List.of(new PokemonSpeciesResponse.FlavorTextEntry("descripcion",
                        new PokemonSpeciesResponse.Language("es"))),
                new PokemonSpeciesResponse.EvolutionChain("https://pokeapi.co/api/v2/evolution-chain/143/")));
        when(client.findEvolutionChainById(143)).thenReturn(new EvolutionChainResponse(
                new EvolutionChainResponse.ChainLink(new PokemonReference("snorlax", "u-snorlax"), null)));

        var detail = service.findByNameOrId("snorlax");

        assertNull(detail.description());
        assertEquals("snorlax", detail.evolutionLineage().name());
        assertEquals(0, detail.evolutionLineage().evolvesTo().size());
    }

    // --- createPokemon ---

    @Test
    void shouldCreatePokemonSuccessfully() {
        var request = new PokemonCreateRequest(25, "pikachu", "img.png", 0.4, 6.0,
                "Electric mouse.", List.of(new PokemonStat("hp", 35)), List.of(), null, null, null);
        PokemonEntity saved = new PokemonEntity(25, "pikachu", "img.png", 0.4, 6.0, "Electric mouse.", Instant.now());
        when(pokemonRepository.existsById(25)).thenReturn(false);
        when(pokemonRepository.findByName("pikachu")).thenReturn(Optional.empty());
        when(pokemonRepository.save(any())).thenReturn(saved);

        PokemonEntity result = service.createPokemon(request);

        assertEquals(25, result.getId());
        assertEquals("pikachu", result.getName());
        verify(pokemonRepository).save(any());
    }

    @Test
    void shouldThrowWhenCreateIdAlreadyExists() {
        var request = new PokemonCreateRequest(25, "pikachu", null, null, null, null, List.of(), List.of(), null, null, null);
        when(pokemonRepository.existsById(25)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.createPokemon(request));
    }

    @Test
    void shouldThrowWhenCreateNameAlreadyExists() {
        var request = new PokemonCreateRequest(999, "pikachu", null, null, null, null, List.of(), List.of(), null, null, null);
        PokemonEntity existing = new PokemonEntity(25, "pikachu", null, null, null, null, Instant.now());
        when(pokemonRepository.existsById(999)).thenReturn(false);
        when(pokemonRepository.findByName("pikachu")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.createPokemon(request));
    }

    @Test
    void shouldThrowWhenCreateRequestHasNullId() {
        var request = new PokemonCreateRequest(null, "pikachu", null, null, null, null, List.of(), List.of(), null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.createPokemon(request));
    }

    @Test
    void shouldThrowWhenCreateRequestHasBlankName() {
        var request = new PokemonCreateRequest(1, "  ", null, null, null, null, List.of(), List.of(), null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.createPokemon(request));
    }

    @Test
    void shouldCreatePokemonWithMetadataWhenProvided() {
        var request = new PokemonCreateRequest(1, "bulbasaur", null, 0.7, 6.9, null,
                List.of(), List.of(), "Fushigidane", "Kanto", "STARTER");
        PokemonEntity saved = new PokemonEntity(1, "bulbasaur", null, 0.7, 6.9, null, Instant.now());
        when(pokemonRepository.existsById(1)).thenReturn(false);
        when(pokemonRepository.findByName("bulbasaur")).thenReturn(Optional.empty());
        when(pokemonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PokemonEntity result = service.createPokemon(request);

        assertNotNull(result.getMetadata());
        assertEquals("Fushigidane", result.getMetadata().getLocalizedName());
        assertEquals("Kanto", result.getMetadata().getRegion());
        assertEquals("STARTER", result.getMetadata().getClassificationTag());
    }

    // --- updatePokemon ---

    @Test
    void shouldUpdatePokemonNameSuccessfully() {
        PokemonEntity existing = new PokemonEntity(1, "bulbasaur", "img.png", 0.7, 6.9, "desc", Instant.now());
        when(pokemonRepository.findById(1)).thenReturn(Optional.of(existing));
        when(pokemonRepository.findByName("bulbasaur-v2")).thenReturn(Optional.empty());
        when(pokemonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new PokemonUpdateRequest("bulbasaur-v2", null, null, null, null,
                List.of(), List.of(), null, null, null);
        PokemonEntity result = service.updatePokemon(1, request);

        assertEquals("bulbasaur-v2", result.getName());
    }

    @Test
    void shouldThrowWhenUpdatePokemonNotFound() {
        when(pokemonRepository.findById(99)).thenReturn(Optional.empty());

        var request = new PokemonUpdateRequest("new-name", null, null, null, null,
                List.of(), List.of(), null, null, null);

        assertThrows(NoSuchElementException.class, () -> service.updatePokemon(99, request));
    }

    @Test
    void shouldThrowWhenUpdateIdIsInvalid() {
        var request = new PokemonUpdateRequest("name", null, null, null, null,
                List.of(), List.of(), null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.updatePokemon(0, request));
    }

    @Test
    void shouldThrowWhenUpdateRequestHasNoFields() {
        var request = new PokemonUpdateRequest(null, null, null, null, null,
                List.of(), List.of(), null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.updatePokemon(1, request));
    }

    @Test
    void shouldUpdateMetadataWhenProvided() {
        PokemonEntity existing = new PokemonEntity(1, "bulbasaur", null, 0.7, 6.9, null, Instant.now());
        when(pokemonRepository.findById(1)).thenReturn(Optional.of(existing));
        when(pokemonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new PokemonUpdateRequest(null, null, null, null, null,
                List.of(), List.of(), "Fushigidane", "Kanto", null);
        PokemonEntity result = service.updatePokemon(1, request);

        assertNotNull(result.getMetadata());
        assertEquals("Fushigidane", result.getMetadata().getLocalizedName());
        assertEquals("Kanto", result.getMetadata().getRegion());
    }

    // --- deletePokemon ---

    @Test
    void shouldDeletePokemonSuccessfully() {
        PokemonEntity existing = new PokemonEntity(1, "bulbasaur", null, 0.7, 6.9, null, Instant.now());
        when(pokemonRepository.findById(1)).thenReturn(Optional.of(existing));

        service.deletePokemon(1);

        verify(pokemonRepository).delete(existing);
    }

    @Test
    void shouldThrowWhenDeletePokemonNotFound() {
        when(pokemonRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.deletePokemon(99));
    }

    @Test
    void shouldThrowWhenDeleteIdIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> service.deletePokemon(-1));
        assertThrows(IllegalArgumentException.class, () -> service.deletePokemon(null));
    }
}
