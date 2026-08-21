package com.pokemon.bff.dto;

public record RegisterRequest(
        String username,
        String email,
        String password
) {
}
