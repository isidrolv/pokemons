package com.pokemon.bff.controller;

import com.pokemon.bff.dto.PokemonCreateRequest;
import com.pokemon.bff.dto.PokemonDetail;
import com.pokemon.bff.dto.PokemonPage;
import com.pokemon.bff.dto.PokemonUpdateRequest;
import com.pokemon.bff.persistence.entity.PokemonEntity;
import com.pokemon.bff.service.PokemonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
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

    @Operation(summary = "List Pokemon from the external API", description = "Returns a paginated page of Pokemon records from the upstream PokeAPI.")
    @GetMapping
    public ResponseEntity<PokemonPage> findAll(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, between 1 and 100") @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.findPage(page, size));
    }

    @Operation(summary = "Get a Pokemon by name or id", description = "Queries the external PokeAPI and returns the full detail payload for the selected Pokemon.")
    @GetMapping("/{pokemon}")
    public ResponseEntity<PokemonDetail> findByNameOrId(@PathVariable String pokemon) {
        if (pokemon == null || pokemon.trim().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.findByNameOrId(pokemon));
    }

    @Operation(summary = "Create a Pokemon in the local database", description = "Creates a record in the local relational store only. This endpoint does not call the external PokeAPI.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pokemon created successfully in the local database"),
            @ApiResponse(responseCode = "400", description = "Malformed request payload, invalid values, or duplicate local identifier", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PokemonEntity> createPokemon(@RequestBody PokemonCreateRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.createPokemon(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update a local Pokemon", description = "Updates an existing row in the local relational store. This operation does not affect the upstream PokeAPI.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pokemon updated successfully"),
            @ApiResponse(responseCode = "400", description = "Malformed payload or invalid values", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pokemon not found in the local store", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<PokemonEntity> updatePokemon(@PathVariable Integer id, @RequestBody PokemonUpdateRequest request) {
        try {
            return ResponseEntity.ok(service.updatePokemon(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Delete a local Pokemon", description = "Deletes an existing row from the local relational store only. This endpoint does not remove data from the external upstream.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pokemon deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pokemon not found in the local store", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePokemon(@PathVariable Integer id) {
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
