# user-service

## Responsibility and boundary

Current responsibility: user account persistence, registration, credential authentication, access/refresh token handling, and authenticated current-user lookup. No other service currently exists. Workspace membership, document lifecycle, realtime collaboration, and notifications are outside the implemented service boundary and remain future direction.

## Stack (source/config verified)

| Concern | Current value | Evidence |
| --- | --- | --- |
| Language | Java 21 | `pom.xml` property `java.version` |
| Framework | Spring Boot 4.1.1 | Maven parent in `pom.xml` |
| Build | Maven; wrapper scripts included | `pom.xml`, `mvnw`, `mvnw.cmd` |
| Web/security | Spring MVC, Spring Security | Maven starters |
| Persistence | Spring Data JPA, Hibernate | Maven starter; `application.yaml` |
| Database/migrations | PostgreSQL; Flyway | Runtime driver and Flyway dependencies; migrations under `src/main/resources/db/migration/` |
| JWT | JJWT 0.12.6 | `pom.xml`; implementation in `security/JwtService.java` |
| Validation | Jakarta Validation | Maven starter and request DTO constraints |
| Operations | Actuator; only `health,info` exposed by web config | `pom.xml`, `application.yaml` |
| HTTP port | 8081 | `src/main/resources/application.yaml` |
| Database | `user_db`, local JDBC URL port 5432 | `application.yaml`, `docker-compose.yml` |

Application entry point: `src/main/java/com/notespace/userservice/UserServiceApplication.java` (sets default JVM timezone to `Europe/Kyiv`).

## Configuration inputs

| Setting | Configured value/source |
| --- | --- |
| `spring.datasource.url` | Hard-coded `jdbc:postgresql://localhost:5432/user_db` in YAML |
| `spring.datasource.username` | `user_service` in YAML |
| `spring.datasource.password` | `password` in YAML and Compose |
| `server.port` | 8081 in YAML |
| `jwt.secret` | Required placeholder `${JWT_SECRET}`; no secret value is documented here |
| `jwt.refreshExpirationMs` | `2592000000` (30 days) in YAML |

The checked-in local database password is a known configuration/security concern; see [[KNOWN-ISSUES]]. No environment-variable configuration for the datasource is defined. Do not copy secrets into docs.

## Important paths

- `src/main/java/com/notespace/userservice/controller/` — HTTP endpoints.
- `service/`, `repository/`, `entity/`, `dto/`, `security/`, `exception/` — application layers; see [[ARCHITECTURE]].
- `src/main/resources/application.yaml` — runtime configuration.
- `src/main/resources/db/migration/` — Flyway schema history.
- `src/test/java/` — current tests; see [[TESTING]].
- `docker-compose.yml` — PostgreSQL only; `Dockerfile` is currently a placeholder, not an application image recipe.
