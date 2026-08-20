# Pokémons — Full-Stack Pokédex

A full-stack application that acts as a **Backend For Frontend (BFF)** for the public [PokéAPI](https://pokeapi.co/). It exposes a clean REST API, persists Pokémon data locally in MySQL, and ships a React/Vite single-page application served directly by the Spring Boot backend.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Key Technologies](#key-technologies)
- [Project Structure](#project-structure)
- [Backend (Spring Boot BFF)](#backend-spring-boot-bff)
  - [API Endpoints](#api-endpoints)
  - [Database Schema](#database-schema)
  - [Configuration](#configuration)
- [Frontend (React + Vite)](#frontend-react--vite)
  - [Components](#components)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Running Locally](#running-locally)
  - [Building the Frontend](#building-the-frontend)
- [Testing](#testing)

---

## Architecture Overview

```
Browser
  │
  │  HTTP (served as static assets)
  ▼
┌────────────────────────────────────────────────────┐
│            Spring Boot BFF  (:8080)                │
│                                                    │
│  PokemonController  →  PokemonService              │
│                              │           │         │
│                      Spring Cache  PokemonRepo     │
│                              │           │         │
│                       PokemonClient  (Spring Data) │
│                         (OpenFeign)       │        │
│                              │            ▼        │
│                         PokéAPI      MySQL DB      │
│                       (external)   + Flyway        │
└────────────────────────────────────────────────────┘
```

The frontend is built as a static bundle that is copied into `src/main/resources/static/` and served by Spring Boot. During local development the Vite dev server proxies API calls to Spring Boot.

---

## Key Technologies

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Backend framework | Spring Boot 4.1 (WebMVC) |
| External API client | Spring Cloud OpenFeign |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8 |
| Database migrations | Flyway (manually configured for Spring Boot 4.1) |
| Caching | Spring Cache with configurable Caffeine / Redis / none |
| API documentation | SpringDoc OpenAPI (Swagger UI) |
| Code generation | Lombok |
| Test data | DataFaker |
| Coverage | JaCoCo |
| Frontend framework | React 19 |
| Frontend build tool | Vite 8 |
| Frontend linter | Oxlint |

---

## Project Structure

```
pokemons/
├── pom.xml                          # Maven build descriptor
├── .env.example                     # Environment variable template
├── WebAppClient/                    # React + Vite single-page application (source)
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.jsx                 # React entry point
│       ├── App.jsx                  # Root component — routing, state, data fetching
│       ├── api/pokemonApi.js        # Fetch helpers that call the BFF REST API
│       └── components/
│           ├── PokemonCard.jsx      # Card rendered for each Pokémon
│           ├── SearchBar.jsx        # Live-filter + exact-search input
│           └── Pagination.jsx       # Page navigation + page-size selector
└── src/
    ├── main/
    │   ├── java/com/pokemon/bff/
    │   │   ├── PokemonApiBffApplication.java   # Spring Boot entry point
    │   │   ├── client/                         # OpenFeign client + response DTOs
    │   │   │   ├── PokemonClient.java
    │   │   │   └── dto/                        # PokéAPI response shapes
    │   │   ├── config/                         # Spring configuration beans
    │   │   │   ├── FlywayConfig.java           # Manual Flyway setup (SB 4.1)
    │   │   │   ├── FlywayMigrationRunner.java
    │   │   │   ├── FlywayMigrationRunnerDetector.java
    │   │   │   ├── OpenApiConfig.java
    │   │   │   ├── PokemonCacheConfiguration.java
    │   │   │   ├── PokemonCacheNames.java
    │   │   │   ├── PokemonCacheProperties.java
    │   │   │   └── WebConfig.java              # CORS / static-resource config
    │   │   ├── controller/
    │   │   │   └── PokemonController.java      # REST endpoints (/api/pokemons)
    │   │   ├── dto/                            # BFF-facing request / response DTOs
    │   │   ├── persistence/
    │   │   │   ├── entity/                     # JPA entities (Pokemon, Stat, Skill, Metadata)
    │   │   │   └── repository/                 # Spring Data repositories
    │   │   ├── service/
    │   │   │   ├── PokemonCacheKeys.java
    │   │   │   └── PokemonService.java         # Business logic, CRUD, PokeAPI aggregation
    │   │   └── sync/
    │   │       └── PokemonSyncService.java     # Writes PokeAPI data into local DB
    │   └── resources/
    │       ├── application.yaml
    │       ├── META-INF/spring.factories       # Registers FlywayMigrationRunnerDetector
    │       ├── db/migration/
    │       │   └── V1__create_pokemon_tables.sql
    │       └── static/                         # Built React app (served by Spring Boot)
    └── test/
        └── java/com/pokemon/bff/
            ├── controller/PokemonControllerTest.java
            ├── service/PokemonServiceTest.java
            └── sync/PokemonSyncServiceTest.java
```

---

## Backend (Spring Boot BFF)

### API Endpoints

All endpoints are under the `/api/pokemons` base path.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/pokemons` | Paginated list of Pokémon (proxied from PokéAPI + synced locally). Query params: `page` (default 0), `size` (default 20, max 100). |
| `GET` | `/api/pokemons/{pokemon}` | Full detail for a Pokémon by name or id, including stats, description, and evolution chain. |
| `POST` | `/api/pokemons` | Create a Pokémon in the local database only (does not call PokéAPI). |
| `PUT` | `/api/pokemons/{id}` | Partially update a locally-stored Pokémon. |
| `DELETE` | `/api/pokemons/{id}` | Delete a locally-stored Pokémon. |

Interactive Swagger UI is available at `http://localhost:8080/swagger-ui.html`.
Actuator health endpoints are exposed at `http://localhost:8080/actuator/health` and also included in the OpenAPI/Swagger view.

### Database Schema

Four tables are created by the Flyway migration `V1__create_pokemon_tables.sql`:

| Table | Purpose |
|---|---|
| `pokemon` | Core fields: id, name, image_url, height, weight, description, synced_at |
| `pokemon_stat` | Base stats (e.g. `hp`, `attack`) linked to a Pokémon |
| `pokemon_skill` | Abilities / skills linked to a Pokémon |
| `pokemon_metadata` | Optional localised name, region, and classification tag |

> **Note on Flyway with Spring Boot 4.1:** `FlywayAutoConfiguration` was removed in Spring Boot 4.1. This project manually registers `FlywayMigrationRunner` (an `InitializingBean`) and `FlywayMigrationRunnerDetector` (a `DatabaseInitializerDetector`) via `META-INF/spring.factories` so that JPA waits for migrations before validating the schema.

### Configuration

Copy `.env.example` to `.env` and fill in the values before starting the application:

```dotenv
DB_URL=jdbc:mysql://localhost:3306/pokemon?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
POKEMON_CACHE_PROVIDER=caffeine
POKEMON_CACHE_PAGE_TTL=5m
POKEMON_CACHE_PAGE_MAXIMUM_SIZE=128
POKEMON_CACHE_DETAIL_TTL=15m
POKEMON_CACHE_DETAIL_MAXIMUM_SIZE=512
```

The `application.yaml` reads these via environment variable substitution:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

pokeapi:
  url: https://pokeapi.co/api/v2
```

Actuator is configured to expose `health` and `info` over HTTP, and `health` always shows component-level details (for example `db` and `redis` when enabled).

Available cache providers:

- `caffeine` — default in-memory cache for local development
- `redis` — shared cache backed by Redis
- `none` — disables caching completely

When Redis is enabled, the backend also reads:

```dotenv
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_TIMEOUT=2s
```

The BFF caches the expensive `GET /api/pokemons` and `GET /api/pokemons/{pokemon}` calls. Pokémon lookup keys are normalized, so `Pikachu`, `pikachu`, and ` pikachu ` reuse the same detail cache entry. Local `POST`, `PUT`, and `DELETE` operations do not evict these caches because the cached read endpoints source data from the upstream PokéAPI rather than the local database.

---

## Frontend (React + Vite)

The frontend is a React 19 single-page application that communicates exclusively with the BFF REST API.

### Components

| Component | Responsibility |
|---|---|
| `App.jsx` | Root component; owns all state, loads pages, runs live filtering and exact search. |
| `SearchBar` | Controlled text input that triggers live filtering on every keystroke and an exact-search fetch on form submit. |
| `PokemonCard` | Displays a single Pokémon: sprite, name, subtitle (category or description), mass, and ability/stat tags. |
| `Pagination` | Previous/Next navigation with a page-size selector (10 / 20 / 50). |

**Search behaviour:**

1. Typing in the search bar immediately filters the currently loaded page in memory (live filter).
2. Pressing Enter (or submitting the form) calls `GET /api/pokemons/{name}` for an exact match with full details including evolution chain.
3. Clearing the input returns to the paginated list view.

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8+
- Node.js 20+ (for local frontend development only)

### Running Locally

1. **Set up the database** — create a MySQL database (or let the JDBC URL create it automatically with `createDatabaseIfNotExist=true`).

2. **Configure environment variables** — copy `.env.example` to `.env` and update the credentials, then export them in your shell:
   ```bash
   export DB_URL=jdbc:mysql://localhost:3306/pokemon?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   export DB_USERNAME=root
   export DB_PASSWORD=secret
   ```

3. **Start the backend:**
   ```bash
   ./mvnw spring-boot:run
   ```
   The application starts on `http://localhost:8080`. Flyway will automatically create the tables on the first run.

4. **Open the app** — navigate to `http://localhost:8080` to use the pre-built React UI.

### Building the Frontend

The React app in `WebAppClient/` must be built and its output copied to `src/main/resources/static/` before the backend is packaged.

```bash
cd WebAppClient
npm install
npm run build
# copy dist/* to ../src/main/resources/static/
cp -r dist/* ../src/main/resources/static/
```

For local frontend development with hot-module reload:

```bash
cd WebAppClient
npm install
npm run dev          # starts Vite dev server, proxies /api to Spring Boot
```

---

## Testing

Run all backend tests with Maven:

```bash
./mvnw test
```

A JaCoCo coverage report is generated at `target/site/jacoco/index.html` during the `verify` phase:

```bash
./mvnw verify
```

Frontend linting:

```bash
cd WebAppClient
npm run lint
```
