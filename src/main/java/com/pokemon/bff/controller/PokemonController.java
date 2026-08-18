package com.pokemon.bff.controller;

import com.pokemon.bff.dto.PokemonDetail;
import com.pokemon.bff.dto.PokemonPage;
import com.pokemon.bff.service.PokemonService;
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
}
