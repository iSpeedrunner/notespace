# Repository conventions

Observed conventions and explicit architecture rules; service-specific Java details live in [user-service conventions](../../services/user-service/docs/context/CONVENTIONS.md).

- Service directories use `services/<service-name>/`; existing service name is lowercase kebab-case.
- Keep each service's capability and data in that service. Do not share database tables or connect directly to another service's database.
- Document public APIs and inter-service contracts at their owning service/system context pages; update context when documented behavior changes.
- Current Java services use Maven, Spring Boot, YAML configuration, package-by-layer organization, JPA, and Flyway SQL migrations. Follow existing setup until an explicit repository convention supersedes it.
- Configuration belongs in application configuration/environment-backed placeholders. Never copy credentials or secrets into context docs.
- Add tests alongside the behavior changed; the current service has JUnit 5/Mockito unit tests and one Spring context test. See [[ROADMAP]] for gaps.
- Prefer constructor injection and existing DTO/validation/exception patterns within `user-service`; see its conventions page.
- Do not introduce brokers, Redis, gateways, orchestration, or extra services solely because they appear on the roadmap.
