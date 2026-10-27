# Evidence-based findings

This is a documentation-only inventory. No implementation issue was changed as part of creating this context.

## Security / configuration

**Issue:** Database credentials are committed as literal defaults/configuration.

**Evidence:** `src/main/resources/application.yaml` contains datasource username `user_service` and password `password`; `docker-compose.yml` repeats credentials. `jwt.secret` is externalized as `${JWT_SECRET}`.

**Affected files:** `src/main/resources/application.yaml`, `docker-compose.yml`.

**Impact:** The checked-in credentials should be treated as local-development configuration only; deployment configuration is not externalized/documented for DB connection.

## API / maintainability

**Issue:** Admin probe route contains a likely spelling error.

**Evidence:** `AdminController` maps `/api/v1/admintets`.

**Affected files:** `src/main/java/com/notespace/userservice/controller/AdminController.java`.

**Impact:** Clients must use the literal path; any correction would change API behavior and needs an explicit compatibility decision.

## Database mapping

**Issue:** User timestamp Java type is local-time while SQL columns are timezone-aware.

**Evidence:** Migration V1 declares `TIMESTAMP WITH TIME ZONE`; `User.createdAt`/`updatedAt` use `LocalDateTime` and are set using `LocalDateTime.now()`.

**Affected files:** `src/main/resources/db/migration/V1___create_users_table.sql`, `src/main/java/com/notespace/userservice/entity/User.java`.

**Impact:** Timezone interpretation/conversion may differ from intended instant semantics; schema validation alone does not demonstrate correct round-trip behavior.

## Consistency / identity normalization

**Issue:** Registration and login do not normalize email consistently.

**Evidence:** `AuthService.register` checks and persists `req.email()` unchanged; `AuthService.login` trims and lowercases before authentication/lookup. Database uniqueness is case-sensitive unless collation/configuration says otherwise.

**Affected files:** `src/main/java/com/notespace/userservice/service/AuthService.java`, migration V1.

**Impact:** Case/whitespace variants may register as distinct values but authenticate against a normalized value that does not match the stored email.

## Token lifecycle

**Issue:** Logout revocation does not reject expired or already revoked tokens.

**Evidence:** `RefreshTokenService.revokeToken` finds by hash and sets `revoked = true` without checking expiry/current revoked state; `verifyToken` performs those checks only on rotation.

**Affected files:** `src/main/java/com/notespace/userservice/service/RefreshTokenService.java`.

**Impact:** Logout behavior is idempotent for a found token, including expired tokens; it does not invalidate access tokens. Record as current behavior, not a fix in this docs task.

## Containerization / delivery

**Issue:** Application Dockerfile is not an application image build, and no CI config exists.

**Evidence:** `Dockerfile` uses `ubuntu:latest` with `ENTRYPOINT ["top", "-b"]`; repository has no GitHub Actions/workflow files. Compose defines only PostgreSQL.

**Affected files:** `Dockerfile`, `docker-compose.yml`; repository root.

**Impact:** Roadmap production containerization and CI/CD are not complete. See root [[ROADMAP]].

## Testing

**Issue:** No endpoint/security/database integration tests found.

**Evidence:** Test tree contains two Mockito service test classes and one Spring context-load test.

**Affected files:** `src/test/java/`.

**Impact:** HTTP contracts, security filter behavior, and Flyway/JPA integration are not directly verified by repository tests.
