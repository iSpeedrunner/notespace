# user-service database

## Configuration and schema source

PostgreSQL database `user_db`, JDBC URL `jdbc:postgresql://localhost:5432/user_db`, configured user `user_service`; local container is defined in `docker-compose.yml` using PostgreSQL 17 and persists named volume `user_postgres_data`. `spring.jpa.hibernate.ddl-auto: validate`; Flyway is enabled. Flyway migrations under `src/main/resources/db/migration/` are the schema change history.

Connection settings are hard-coded for local development in `application.yaml` / Compose rather than externalized. See [[KNOWN-ISSUES]].

## Migrations and tables

| Migration | Effect |
| --- | --- |
| `V1___create_users_table.sql` | `users`: UUID PK; required unique `username` VARCHAR(50), `email` VARCHAR(255), `password_hash` VARCHAR(255); required `created_at`, `updated_at` as `TIMESTAMP WITH TIME ZONE`. |
| `V2___add_user_role.sql` | Adds `users.role` VARCHAR(20) NOT NULL DEFAULT `'USER'`; no DB check constraint on allowed values. |
| `V3___create_refreshtoken_table.sql` | `refresh_token`: UUID PK; required `user_id`, unique required `token_hash`, required `expires_at`, `revoked` default false, required `created_at`; FK to `users(id)`. No explicit index beyond PK/unique constraints. |

## JPA mapping comparison

- `User` maps to `users`; columns and unique/nullability declarations correspond to V1/V2. Timestamps are `LocalDateTime` while migration columns are `TIMESTAMP WITH TIME ZONE`; Java/JDBC timezone semantics require review. `@PrePersist`/`@PreUpdate` assign local date-time values.
- `RefreshToken` maps to `refresh_token`; `Instant` fields correspond to timezone-aware timestamp columns; `user` is lazy `ManyToOne` and maps the V3 FK.
- UUIDs are generated through JPA. Migration declares UUID PKs without SQL defaults, which is consistent with application-side generation.
- Hibernate validates schema at startup; no integration test verifies migration-to-entity compatibility.

Detailed discrepancy is tracked in [[KNOWN-ISSUES]].
