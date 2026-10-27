# user-service conventions

Observed patterns in current source; inconsistencies are recorded in [[KNOWN-ISSUES]].

- Packages are organized by layer: `controller`, `service`, `repository`, `entity`, `dto`, `security`, `exception`.
- Services/controllers mostly use Lombok `@AllArgsConstructor` constructor injection; `RefreshTokenService` uses an explicit constructor. Continue constructor injection.
- Controllers use Spring MVC annotations and DTOs; auth request bodies use Jakarta constraints and `@Valid`. Responses use Java records.
- Application/domain errors use dedicated runtime exceptions and `GlobalExceptionHandler`; keep API error shape changes aligned with [[API]]. Security 401/403 use separate bare `sendError` handlers.
- Repositories are Spring Data JPA interfaces. Services perform repository access; controllers do not.
- `AuthService.register`, `RefreshTokenService.rotateRefreshToken`, and `revokeToken` use Jakarta `@Transactional`; transaction annotation usage is not fully uniform.
- Migrations are ordered Flyway files `V<number>__description.sql` (actual names use triple underscores after version); schema is validated by Hibernate. Add schema changes as new migrations rather than editing applied history.
- Unit tests use JUnit 5, Mockito, and Spring test utilities. See [[TESTING]].
- Logging is not used in the inspected application classes; avoid logging credentials or raw tokens if logging is later added.
