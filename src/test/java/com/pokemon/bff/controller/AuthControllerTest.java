package com.pokemon.bff.controller;

import com.pokemon.bff.dto.AuthResponse;
import com.pokemon.bff.dto.LoginRequest;
import com.pokemon.bff.dto.RegisterRequest;
import com.pokemon.bff.service.AuthService;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    private final Faker faker = new Faker();

    @Test
    void shouldReturn201WhenRegisterIsValid() {
        // Given a valid registration request
        var request = new RegisterRequest(faker.internet().username(), faker.internet().emailAddress(), "supersecret1");
        var expected = new AuthResponse("jwt-token", request.username());
        when(authService.register(request)).thenReturn(expected);

        // when the controller is called
        ResponseEntity<AuthResponse> response = controller.register(request);

        // then it should return 201 with the token
        assertEquals(201, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void shouldReturn400WhenRegisterThrowsIllegalArgument() {
        // Given a request rejected by the service (e.g. duplicate username)
        var request = new RegisterRequest(faker.internet().username(), faker.internet().emailAddress(), "supersecret1");
        when(authService.register(request)).thenThrow(new IllegalArgumentException("Username is already taken"));

        // when the controller is called
        ResponseEntity<AuthResponse> response = controller.register(request);

        // then it should return 400
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void shouldReturn200WhenLoginIsValid() {
        // Given valid credentials
        var request = new LoginRequest(faker.internet().username(), "supersecret1");
        var expected = new AuthResponse("jwt-token", request.username());
        when(authService.login(request)).thenReturn(expected);

        // when the controller is called
        ResponseEntity<AuthResponse> response = controller.login(request);

        // then it should return 200 with the token
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void shouldReturn401WhenLoginHasBadCredentials() {
        // Given credentials rejected by the authentication manager
        var request = new LoginRequest(faker.internet().username(), "wrong-password");
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        // when the controller is called
        ResponseEntity<AuthResponse> response = controller.login(request);

        // then it should return 401
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void shouldReturn400WhenLoginThrowsIllegalArgument() {
        // Given a malformed login payload
        var request = new LoginRequest(" ", " ");
        when(authService.login(request)).thenThrow(new IllegalArgumentException("Username and password must not be blank"));

        // when the controller is called
        ResponseEntity<AuthResponse> response = controller.login(request);

        // then it should return 400
        assertEquals(400, response.getStatusCode().value());
    }
}
