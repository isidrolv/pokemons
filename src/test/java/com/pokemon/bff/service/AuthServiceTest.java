package com.pokemon.bff.service;

import com.pokemon.bff.dto.AuthResponse;
import com.pokemon.bff.dto.LoginRequest;
import com.pokemon.bff.dto.RegisterRequest;
import com.pokemon.bff.persistence.entity.UserEntity;
import com.pokemon.bff.persistence.repository.UserRepository;
import com.pokemon.bff.security.JwtService;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService service;

    private final Faker faker = new Faker();

    @Test
    void shouldRegisterNewUserAndReturnToken() {
        // Given a valid registration request for a username that does not exist yet
        String username = faker.internet().username();
        String email = faker.internet().emailAddress();
        String rawPassword = "supersecret1";
        String encodedPassword = "encoded-" + rawPassword;
        String token = "jwt-token";
        var request = new RegisterRequest(username, email, rawPassword);

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(jwtService.generateToken(username)).thenReturn(token);

        // when registering the user
        AuthResponse response = service.register(request);

        // then the user should be persisted with an encoded password and a token returned
        verify(userRepository).save(any(UserEntity.class));
        assertEquals(token, response.token());
        assertEquals(username, response.username());
    }

    @Test
    void shouldRejectRegistrationWithDuplicateUsername() {
        // Given a username that already exists
        String username = faker.internet().username();
        var request = new RegisterRequest(username, faker.internet().emailAddress(), "supersecret1");
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // when registering with that username
        // then it should be rejected
        assertThrows(IllegalArgumentException.class, () -> service.register(request));
    }

    @Test
    void shouldRejectRegistrationWithShortPassword() {
        // Given a password shorter than the minimum length
        var request = new RegisterRequest(faker.internet().username(), faker.internet().emailAddress(), "short");

        // when registering
        // then it should be rejected before touching the repository
        assertThrows(IllegalArgumentException.class, () -> service.register(request));
    }

    @Test
    void shouldLoginWithValidCredentialsAndReturnToken() {
        // Given valid credentials
        String username = faker.internet().username();
        String token = "jwt-token";
        var request = new LoginRequest(username, "supersecret1");
        when(jwtService.generateToken(username)).thenReturn(token);

        // when logging in
        AuthResponse response = service.login(request);

        // then a token should be returned for the authenticated user
        assertEquals(token, response.token());
        assertEquals(username, response.username());
    }

    @Test
    void shouldRejectLoginWithBadCredentials() {
        // Given invalid credentials rejected by the authentication manager
        var request = new LoginRequest(faker.internet().username(), "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        // when logging in
        // then it should propagate as bad credentials
        assertThrows(BadCredentialsException.class, () -> service.login(request));
    }

    @Test
    void shouldRejectLoginWithBlankPassword() {
        // Given a blank password
        var request = new LoginRequest(faker.internet().username(), "  ");

        // when logging in
        // then it should be rejected before contacting the authentication manager
        assertThrows(IllegalArgumentException.class, () -> service.login(request));
    }
}
