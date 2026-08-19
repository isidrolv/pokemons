package com.pokemon.bff.service;

import java.util.Locale;

public final class PokemonCacheKeys {
    private PokemonCacheKeys() {
    }

    public static String page(int page, int size) {
        return page + ":" + size;
    }

    public static String pokemon(String pokemon) {
        return pokemon == null ? "" : pokemon.trim().toLowerCase(Locale.ROOT);
    }
}
