# Pokemon API BFF

Backend-for-Frontend para consultar Pokémon mediante Spring Boot, Spring Web, Spring Data JPA y OpenFeign. La aplicación también puede servir el frontend React generado con Vite desde `src/main/resources/static`.

## Requisitos

- Java 21
- Maven Wrapper (`mvnw.cmd`)
- Node.js y npm para construir el frontend
- MySQL con las variables `DB_USERNAME` y `DB_PASSWORD`

## Ejecutar el backend

```powershell
.\mvnw.cmd spring-boot:run
```

La API estará disponible en `http://localhost:8080`.

## Frontend React

El frontend se encuentra en `WebAppClient` y usa React con Vite:

```powershell
cd WebAppClient
npm install
npm run build
cd ..
```

El build debe generarse en `src/main/resources/static` mediante la configuración de Vite. Después puede ejecutarse Spring Boot para servir la aplicación completa desde el mismo origen.

## API principal

```text
GET    /api/pokemons?page=0&size=20
GET    /api/pokemons/{name-or-id}
POST   /api/pokemons
PUT    /api/pokemons/{id}
DELETE /api/pokemons/{id}
```

Las operaciones `POST`, `PUT` y `DELETE` trabajan sobre la base de datos local. La actualización devuelve `400` para identificadores o payloads inválidos y `404` cuando el Pokémon no existe localmente.

La documentación OpenAPI está disponible en:

```text
http://localhost:8080/swagger-ui/index.html
```

## Base de datos

Las migraciones Flyway están en `src/main/resources/db/migration`. La configuración por defecto usa MySQL:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "tu-password"
```

También puede sobrescribirse la URL con `DB_URL`.

## Pruebas y empaquetado

```powershell
.\mvnw.cmd clean verify
java -jar target\pokemon-api-bff-0.0.1-SNAPSHOT.jar
```

## Estructura

```text
src/main/java/com/pokemon/bff
├── client/                  # Cliente Feign para PokeAPI
├── controller/              # Endpoints HTTP
├── dto/                     # DTOs de entrada y salida
├── persistence/entity/      # Entidades JPA
├── persistence/repository/  # Repositorios Spring Data
├── service/                 # Lógica de negocio
└── sync/                    # Sincronización con PokeAPI

WebAppClient/                # Aplicación React + Vite
```
