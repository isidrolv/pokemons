package com.pokemon.bff.security;

import net.datafaker.Faker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {
    private static final String TEST_SECRET = "dev-only-secret-please-change-me-1234567890abcdef";

    private final Faker faker = new Faker();

    @Test
    void shouldGenerateAndValidateToken() {
        // Given a service with a valid secret and expiration
        JwtService service = new JwtService(TEST_SECRET, 60_000L);
        String username = faker.internet().username();

        // when a token is generated for a user
        String token = service.generateToken(username);

        // then the token should be valid and carry the username
        assertTrue(service.isValid(token));
        assertEquals(username, service.extractUsername(token));
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        // Given a service with an already-expired token
        JwtService service = new JwtService(TEST_SECRET, 1L);
        String token = service.generateToken(faker.internet().username());
        Thread.sleep(10);

        // when checking validity after expiration
        // then it should be considered invalid
        assertFalse(service.isValid(token));
    }

    @Test
    void shouldRejectMalformedToken() {
        // Given a service and a garbage token
        JwtService service = new JwtService(TEST_SECRET, 60_000L);

        // when checking validity of a malformed token
        // then it should be considered invalid
        assertFalse(service.isValid("not-a-real-token"));
    }
}
