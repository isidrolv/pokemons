package com.pokemon.bff.controller;

import com.pokemon.bff.dto.PokemonCreateRequest;
import com.pokemon.bff.dto.PokemonDetail;
import com.pokemon.bff.dto.PokemonPage;
import com.pokemon.bff.dto.PokemonUpdateRequest;
import com.pokemon.bff.persistence.entity.PokemonEntity;
import com.pokemon.bff.service.PokemonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

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

    @GetMapping("/search")
    public ResponseEntity<PokemonPage> searchPokemons(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (query == null || query.trim().isBlank() || page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.searchPokemonsWhichContains(query, page, size));
    }

    @PostMapping
    public ResponseEntity<PokemonEntity> createPokemon(@RequestBody PokemonCreateRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            PokemonEntity created = service.createPokemon(request);
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PokemonEntity> updatePokemon(@PathVariable Integer id, @RequestBody PokemonUpdateRequest request) {
        if (id == null || id <= 0 || request == null) {
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
    public ResponseEntity<Void> deletePokemon(@PathVariable Integer id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        try {
            service.deletePokemon(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
