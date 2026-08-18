CREATE TABLE pokemon (
    id          INT             NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    image_url   VARCHAR(512),
    height      DOUBLE,
    weight      DOUBLE,
    description TEXT,
    synced_at   TIMESTAMP(6)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_pokemon_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE pokemon_stat (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    pokemon_id  INT             NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    value       INT             NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_stat_pokemon FOREIGN KEY (pokemon_id) REFERENCES pokemon (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE pokemon_skill (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    pokemon_id  INT             NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    url         VARCHAR(512),
    PRIMARY KEY (id),
    CONSTRAINT fk_skill_pokemon FOREIGN KEY (pokemon_id) REFERENCES pokemon (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE pokemon_metadata (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    pokemon_id          INT             NOT NULL,
    localized_name      VARCHAR(200),
    region              VARCHAR(100),
    classification_tag  VARCHAR(100),
    PRIMARY KEY (id),
    UNIQUE KEY uq_metadata_pokemon (pokemon_id),
    CONSTRAINT fk_metadata_pokemon FOREIGN KEY (pokemon_id) REFERENCES pokemon (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
