# Pokemon API BFF

Backend-for-Frontend construido con Spring Boot, Spring Web, Spring Data JPA y OpenFeign. El proyecto expone una API para consultar y administrar información de Pokémon, además de servir el frontend React generado por Vite.

## Requisitos

- Java 21
- Maven Wrapper incluido (`mvnw.cmd`)
- Node.js y npm, necesarios para construir el frontend
- MySQL disponible para el perfil de ejecución configurado

## Estructura principal

```text
src/main/java/com/pokemon/bff
├── client/                  # Cliente Feign para PokeAPI
├── controller/              # Endpoints HTTP
├── dto/                     # Objetos de entrada y salida
├── persistence/
│   ├── entity/              # Entidades JPA
│   └── repository/          # Repositorios Spring Data
├── service/                 # Lógica de negocio
└── sync/                    # Sincronización con PokeAPI

WebAppClient/                # Aplicación React + Vite
src/main/resources/static/   # Build del frontend servido por Spring Boot
src/main/resources/db/       # Migraciones Flyway
```

## Repositorios

`PokemonRepository` extiende `JpaRepository<PokemonEntity, Integer>`, por lo que hereda operaciones CRUD, paginación y ordenamiento para `PokemonEntity`.

También declara:

```java
Optional<PokemonEntity> findByName(String name);
```

Spring Data JPA genera automáticamente la consulta por el nombre del Pokémon.

## Ejecutar en desarrollo

Construir el frontend:

```powershell
cd WebAppClient
npm install
npm run build
cd ..
```

Ejecutar Spring Boot:

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación estará disponible en:

```text
http://localhost:8080
```

## Construir el proyecto completo

El build de Vite debe generar sus archivos en `src/main/resources/static`. Después empaqueta la aplicación:

```powershell
.\mvnw.cmd clean package
```

Ejecuta el JAR resultante con:

```powershell
java -jar target\pokemon-api-bff-0.0.1-SNAPSHOT.jar
```

## API principal

```text
GET    /api/pokemons?page=0&size=20
GET    /api/pokemons/{name-or-id}
POST   /api/pokemons
PUT    /api/pokemons/{id}
DELETE /api/pokemons/{id}
```

La documentación OpenAPI, si la aplicación está ejecutándose, está disponible en:

```text
http://localhost:8080/swagger-ui/index.html
```

## Base de datos

Las migraciones se encuentran en `src/main/resources/db/migration`. Flyway las ejecuta al iniciar la aplicación cuando la configuración de base de datos está disponible.

## Pruebas

```powershell
.\mvnw.cmd test
```

## Notas

- Las llamadas del frontend deben usar rutas relativas, por ejemplo `fetch('/api/pokemons')`.
- `PokemonRepository` debe utilizarse desde la capa de servicio, no directamente desde el controlador.
- No se deben editar manualmente los archivos generados dentro de `src/main/resources/static/assets`; deben regenerarse con `npm run build`.
