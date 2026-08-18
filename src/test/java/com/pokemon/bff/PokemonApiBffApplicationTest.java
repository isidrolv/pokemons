package com.pokemon.bff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PokemonApiBffApplicationTest {

    @Test
    void testMain() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                PokemonApiBffApplication.main(new String[]{}));
    }
}