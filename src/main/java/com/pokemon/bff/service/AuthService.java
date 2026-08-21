package com.pokemon.bff.service;

import com.pokemon.bff.dto.AuthResponse;
import com.pokemon.bff.dto.LoginRequest;
import com.pokemon.bff.dto.RegisterRequest;
import com.pokemon.bff.persistence.entity.UserEntity;
import com.pokemon.bff.persistence.repository.UserRepository;
import com.pokemon.bff.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        String username = request.username().trim();
        String email = request.email().trim();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        UserEntity user = new UserEntity(username, email, passwordEncoder.encode(request.password()),
                DEFAULT_ROLE, Instant.now());
        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(username), username);
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Username and password must not be blank");
        }

        String username = request.username().trim();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.password()));

        return new AuthResponse(jwtService.generateToken(username), username);
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration payload must not be null");
        }
        if (request.username() == null || request.username().isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (request.password() == null || request.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
    }
}
