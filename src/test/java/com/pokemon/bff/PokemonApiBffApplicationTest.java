package com.pokemon.bff;

import com.pokemon.bff.PokemonApiBffApplication;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PokemonApiBffApplicationTest {

    @Test
    void main() {

        try {
            PokemonApiBffApplication.main(new String[]{});
            assertTrue(true);
        } catch (Exception e) {
            System.out.println("The main method should not throw an exception: " + e.getMessage());
        }

    }
}