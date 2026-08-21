package com.pokemon.bff.dto;

public record AuthResponse(
        String token,
        String username
) {
}
