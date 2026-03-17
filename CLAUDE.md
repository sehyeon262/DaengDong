# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack mobile application with a Spring Boot 3.5 REST API backend and an Android (Kotlin + Jetpack Compose) frontend. Deployed via Jenkins CI/CD to separate dev/prod environments using Docker.

## Build & Run Commands

### Backend (Spring Boot — Java 17)

```bash
cd backend

# Run locally
./gradlew bootRun

# Build JAR (skip tests)
./gradlew clean build -x test

# Run tests
./gradlew test
```

Requires a `.env` file in `backend/` with:
```
DB_URL=jdbc:postgresql://localhost:5432/e108_db_dev
DB_PASSWORD=<password>
JWT_SECRET=<32+ char secret>
SPRING_PROFILES_ACTIVE=dev
```

### Frontend (Android)

```bash
cd frontend
./gradlew assembleDebug   # Debug APK
./gradlew build            # Full build
```

### Docker (Dev / Prod)

```bash
# Development (ports: DB=5433, app=8081)
docker compose -f docker-compose-dev.yaml --env-file .env up -d

# Production (ports: DB=5432, app=80)
docker compose -f docker-compose-prod.yaml --env-file .env up -d
```

## Architecture

### Backend — Domain-Driven Layered Structure

```
com.e108.be/
├── domain/
│   └── {feature}/
│       ├── controller/     # REST endpoints → returns ResTemplate<T>
│       ├── service/        # Business logic (@Transactional)
│       ├── repository/     # JPA (JpaRepository<T, ID>)
│       ├── entity/         # JPA entities (extend BaseEntity)
│       ├── dto/
│       │   ├── request/    # @Getter @NoArgsConstructor
│       │   └── response/   # @Getter @Builder with static from(Entity)
│       └── exception/      # Extend appropriate *GroupException
│
└── global/
    ├── common/
    │   ├── template/       # ResTemplate — unified API response wrapper
    │   ├── entity/         # BaseEntity: createdAt, updatedAt, isDeleted
    │   └── enums/
    ├── config/             # Security, CORS beans
    ├── error/              # ControllerAdvice + exception group hierarchy
    ├── jwt/                # JwtTokenProvider, JwtAuthenticationFilter
    └── logging/            # Request/response logging filters
```

### Unified API Response Format (ResTemplate)

All controllers wrap responses in `ResTemplate<T>`:
```json
{ "code": 200, "message": "로그인 성공", "data": {} }
{ "code": 204, "message": "삭제 성공" }
```

### Exception Hierarchy

Throw these; `ControllerAdvice` maps them to HTTP status automatically:
- `InvalidGroupException` → 400
- `AuthGroupException` → 401
- `AccessDeniedGroupException` → 403
- `NotFoundGroupException` → 404
- `ConflictGroupException` → 409
- `InternalServerErrorGroupException` → 500

### BaseEntity (all entities extend this)

Provides `createdAt`, `updatedAt` (auto-audited), and `isDeleted` (soft delete). Never hard-delete records; set `isDeleted = true`.

### Service Layer Transactions

```java
@Service
@Transactional(readOnly = true)   // default: read-only
public class SomeService {
    @Transactional                 // explicit on write methods
    public void createSomething() { /* ... */ }
}
```

### Security

- JWT tokens expire in 1 hour; filter runs before `UsernamePasswordAuthenticationFilter`
- `/api/v1/auth/**` is unauthenticated; everything else requires valid JWT
- CORS configured for `http://localhost:3000`
- Stateless sessions, CSRF disabled

### CI/CD (Jenkins)

Pipeline in `backend/Jenkinsfile`:
- `develop` branch → builds dev image (`backend-app-dev`) → deploys via `docker-compose-dev.yaml`
- `main` branch → builds prod image (`backend-app-prod`) → deploys via `docker-compose-prod.yaml`

Credentials injected via Jenkins secret files: `be-dev-env` and `be-prod-env`.

## Conventions (from `backend/CONVENTION.md`)

### Naming

| Type | Pattern | Example |
|------|---------|---------|
| Controller | `{Domain}Controller` | `AuthController` |
| Service | `{Domain}Service` | `AuthService` |
| Repository | `{Entity}Repository` | `MemberRepository` |
| Entity | Singular noun | `Member`, `Board` |
| Request DTO | `{Action}{Domain}Request` | `LoginRequest` |
| Response DTO | `{Domain}Response` | `MemberResponse` |
| Exception | `{Situation}Exception` | `MemberNotFoundException` |

### Git Branches

- `main` — production deployment
- `develop` — integration (default)
- `be/feature/{issue}-{desc}`, `be/fix/{issue}-{desc}`, `be/refactor/{issue}-{desc}`

### Commit Format

```
{TYPE}[#{ISSUE}] {description}
```
Types: `FEAT`, `FIX`, `DOCS`, `STYLE`, `REFACTOR`, `TEST`, `CHORE`, `DESIGN`, `RENAME`, `REMOVE`, `!HOTFIX`, `!BREAKING CHANGE`

### Code Rules

- No `@Setter` on entities
- No `System.out.println` or debug statements
- Entities must not expose setters; use constructors or builder
- Response DTOs must have a `static from(Entity)` factory method
- API base path: `/api/v1`