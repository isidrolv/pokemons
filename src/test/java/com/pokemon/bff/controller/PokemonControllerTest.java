package com.pokemon.bff.controller;

import com.pokemon.bff.dto.*;
import com.pokemon.bff.service.PokemonService;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokemonControllerTest {
    public static final String BLANK_POKEMON_IDENTIFIER = "   ";
    @Mock
    private PokemonService service;

    @InjectMocks
    private PokemonController controller;

    private PokemonFactory pokemonFactory;

    @BeforeEach
    void setUp() {
        pokemonFactory = new PokemonFactory();
    }

    @Test
    void shouldReturnBadRequestWhenPokemonIsBlank() {
        // Given a blank Pokemon name or ID
        // when the controller is called with a blank Pokemon name or ID
        var response = controller.findByNameOrId(BLANK_POKEMON_IDENTIFIER);

        // then the response should be a bad request
        assertEquals(400, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    void validatePaginationParamsInvalidPage() {

        // Given invalid pagination parameters
        int invalidPage = -1;
        int invalidSize = 0;

        // when the controller is called with invalid pagination parameters
        ResponseEntity<PokemonPage> response = controller.findAll(invalidPage, invalidSize);

        // then the response should be a bad request
        assertEquals(400, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    void validatePaginationParamsInvalidSize() {

        // Given invalid pagination parameters
        int pageNumber = 1;
        int invalidSize = 0;

        // when the controller is called with invalid pagination parameters
        ResponseEntity<PokemonPage> response = controller.findAll(pageNumber, invalidSize);

        // then the response should be a bad request
        assertEquals(400, response.getStatusCode().value());
        assertNull(response.getBody());

    }

    @Test
    void validatePaginationParamsInvalidSize2() {

        // Given invalid pagination parameters
        int pageNumber = 1;
        int invalidSize = 200;

        // when the controller is called with invalid pagination parameters
        ResponseEntity<PokemonPage> response = controller.findAll(pageNumber, invalidSize);

        // then the response should be a bad request
        assertEquals(400, response.getStatusCode().value());
        assertNull(response.getBody());

    }

    @Test
    void shouldReturnPokemonPage() {
        // Given a valid page and size
        int page = 0;
        int size = 20;

        // when the controller is called with valid pagination parameters
        var pokemonPage = new PokemonPage(pokemonFactory.getPokemons(), page, size, 100, 5);
        when(service.findPage(page, size)).thenReturn(pokemonPage);
        var response = controller.findAll(page, size);

        // then the response should be a PokemonPage object
        assertEquals(200, response.getStatusCode().value());
        assertEquals(pokemonPage, response.getBody());
    }


    @Test
    void shouldReturnDetailedPokemon() {
        // Given a Pokemon name or ID
        Faker faker = new Faker();
        int pokemonId = faker.number().numberBetween(1, 500);
        String imageUrl = faker.internet().url();
        String description = faker.lorem().sentence();

        when(service.findByNameOrId("pikachu"))
                .thenReturn(new PokemonDetail(
                        pokemonId,
                        "pikachu",
                        imageUrl,
                        0.4,
                        6.0,
                        List.of(),
                        description,
                        new EvolutionNode("pichu", List.of(new EvolutionNode("pikachu", List.of())))));

        // when the controller is called with a Pokemon name or ID
        var response = controller.findByNameOrId("pikachu");

        // then the response should be a detailed view of the Pokemon
        assertEquals(200, response.getStatusCode().value());
        assertEquals(pokemonId, response.getBody().id());
        assertEquals("pikachu", response.getBody().name());
        assertEquals(imageUrl, response.getBody().image());
        assertEquals(description, response.getBody().description());

        // And when pokemon name is null then the response should be a bad request
        var nullResponse = controller.findByNameOrId(null);
        assertEquals(400, nullResponse.getStatusCode().value());
    }


    public static class PokemonFactory {
        public List<PokemonItem> getPokemons() {
            return List.of(
                    new PokemonItem(1, "bulbasaur", "https://pokeapi.co/media/sprites/bulbasaur.png", "earth", 10.5, List.of(new Skill("tackle", "10"))),
                    new PokemonItem(2, "ivysaur", "https://pokeapi.co/media/sprites/ivysaur.png", "earth", 13.0, List.of(new Skill("vine whip", "20"))),
                    new PokemonItem(3, "venusaur", "https://pokeapi.co/media/sprites/venusaur.png", "earth", 100.0, List.of(new Skill("solar beam", "30")))
            );
        }
    }
}
