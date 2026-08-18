package com.pokemon.bff.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pokemon")
@Getter
@Setter
@NoArgsConstructor
public class PokemonEntity {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    private Double height;

    private Double weight;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @OneToMany(mappedBy = "pokemon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PokemonStatEntity> stats = new ArrayList<>();

    @OneToMany(mappedBy = "pokemon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PokemonSkillEntity> skills = new ArrayList<>();

    @OneToOne(mappedBy = "pokemon", cascade = CascadeType.ALL, orphanRemoval = true)
    private PokemonMetadataEntity metadata;

    public PokemonEntity(Integer id, String name, String imageUrl, Double height, Double weight,
            String description, Instant syncedAt) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.height = height;
        this.weight = weight;
        this.description = description;
        this.syncedAt = syncedAt;
    }
}
