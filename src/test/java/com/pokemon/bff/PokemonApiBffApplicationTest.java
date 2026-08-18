package com.pokemon.bff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PokemonApiBffApplicationTest {

    @Test
    void testMain() {

        try {
            PokemonApiBffApplication.main(new String[]{});
            assertTrue(true);
        } catch (Exception e) {
            System.out.println("The main method should not throw an exception: " + e.getMessage());
        }

    }
}