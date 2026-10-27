# user-service domain model

Only account/authentication entities exist. Workspace, membership, document, collaboration, and presence concepts are not modeled here; see system [[PRODUCT]].

## `User`

**Source:** `src/main/java/com/notespace/userservice/entity/User.java` · **Table:** `users`.

| Field | Mapping / rule |
| --- | --- |
| `id` | UUID primary key, generated with JPA UUID strategy |
| `username` | Required, unique, max 50 chars |
| `email` | Required, unique, max 255 chars |
| `passwordHash` | Required, column `password_hash` |
| `role` | Required string enum, max 20; defaults to `USER` |
| `createdAt` | Required, non-updatable; set in `@PrePersist` |
| `updatedAt` | Set in `@PrePersist` and `@PreUpdate` |

Lifecycle: created by `AuthService.register`; role starts as `USER`. Registration checks email/username existence before save. Password is encoded using configured `BCryptPasswordEncoder`. Account deletion/update flows are absent.

## `Role`

**Source:** `src/main/java/com/notespace/userservice/entity/Role.java`. Values: `USER`, `ADMIN`; persisted as string on `users.role`. These are account authorities, not planned workspace membership roles.

## `RefreshToken`

**Source:** `src/main/java/com/notespace/userservice/entity/RefreshToken.java` · **Table:** `refresh_token`.

Fields: UUID `id`; required lazy `ManyToOne` `user` via `user_id`; unique required `token_hash`; required `expires_at`; required `revoked`; required `created_at`. Raw UUID token is returned once from creation; SHA-256 hash is stored. Rotation revokes the old token and issues a new one; logout marks the matching token revoked. Expiry and revoked state are checked during rotation.

## Domain invariants and enforcement

| Invariant | Enforcement found |
| --- | --- |
| Username/email are unique and required | `users` Flyway V1 constraints; JPA `@Column`; registration checks `existsByEmail` / `existsByUsername` and throws conflict exceptions. Exact casing/normalization behavior differs by flow; see [[KNOWN-ISSUES]]. |
| New registered users have role `USER` | `AuthService.register` sets it explicitly; entity builder default and migration default also specify `USER`. |
| Password is not persisted as submitted | Registration calls `PasswordEncoder.encode`; entity stores `passwordHash`. |
| Refresh token lookup uses a hash | `RefreshTokenService` hashes token bytes with SHA-256 before lookup; entity stores hash. |
| Rotated token must be unrevoked and unexpired | `RefreshTokenService.verifyToken` checks both before rotation. Logout revocation itself does not check expiry/revoked status. |

These statements describe code paths, not guarantees against concurrent requests or database race conditions.
