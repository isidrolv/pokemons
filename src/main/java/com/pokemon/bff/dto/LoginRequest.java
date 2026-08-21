package com.pokemon.bff.dto;

public record LoginRequest(
        String username,
        String password
) {
}
