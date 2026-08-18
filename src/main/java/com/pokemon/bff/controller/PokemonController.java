package com.pokemon.bff.controller;

import com.pokemon.bff.dto.PokemonDetail;
import com.pokemon.bff.dto.PokemonCreateRequest;
import com.pokemon.bff.dto.PokemonPage;
import com.pokemon.bff.dto.PokemonUpdateRequest;
import com.pokemon.bff.service.PokemonService;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pokemons")
public class PokemonController {
    private final PokemonService service;

    public PokemonController(PokemonService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PokemonPage> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.findPage(page, size));
    }

    @GetMapping("/{pokemon}")
    public ResponseEntity<PokemonDetail> findByNameOrId(@PathVariable String pokemon) {
        if (pokemon == null || pokemon.trim().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.findByNameOrId(pokemon));
    }

    @PostMapping
    public ResponseEntity<PokemonDetail> createPokemon(@RequestBody(required = false) PokemonCreateRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.createPokemon(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PokemonDetail> updatePokemon(@PathVariable int id,
                                                      @RequestBody(required = false) PokemonUpdateRequest request) {
        if (id < 1) {
            return ResponseEntity.badRequest().build();
        }
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(service.updatePokemon(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePokemon(@PathVariable int id) {
        if (id < 1) {
            return ResponseEntity.badRequest().build();
        }
        try {
            service.deletePokemon(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
