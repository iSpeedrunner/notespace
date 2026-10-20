# Workspace Service Layered Structure

## Status

Approved design for the initial `workspace-service` foundation.

## Goal

Turn the existing `workspace-service` Spring Boot skeleton and entity draft into a small, compilable layered service foundation for workspace lifecycle and membership data, without introducing cross-service database access or infrastructure that is not currently required.

## Context

The repository already contains `services/workspace-service` with Java 21, Spring Boot, PostgreSQL configuration, Flyway on the classpath, and draft entities for workspaces, members, and invitations. The system context identifies `workspace-service` as the planned owner of workspaces, memberships, invitations, workspace authorization, and document metadata/lifecycle. The implementation must not read `user-service`'s database directly.

The current source is only a partial scaffold: `Workspace`, `WorkspaceMember`, and `WorkspaceInvitation` entities exist; repositories, application services, controllers, migrations, and service-local context do not yet exist. The root context still describes `workspace-service` as planned. This design describes the first implementation slice; it does not change source-verified status by itself.

## Terminology

The implementation package and API use `Workspace`, matching the existing source identifiers and the planned service name. The system decision in `docs/context/DECISIONS.md` keeps “Workspace” versus “Room” open; this slice does not introduce a `Room` alias, rename existing identifiers, or resolve that product decision.

## Scope

### In scope

- Establish a local `workspace-service` service guide and README.
- Organize application code into the following layers:
  - `controller` for HTTP adapters;
  - `dto` for request/response models;
  - `service` for workspace use cases;
  - `repository` for persistence ports implemented with Spring Data JPA;
  - `entity` for service-owned persistence/domain state;
  - `exception` for API error mapping;
  - `config` for service-local configuration.
- Normalize the existing workspace, member, and invitation entities.
- Define `WorkspaceRole` with `OWNER` and `MEMBER`.
- Add a Flyway baseline migration for workspace, member, and invitation tables with local constraints and indexes.
- Add the initial workspace lifecycle API:
  - `POST /api/v1/workspaces` — create a workspace;
  - `GET /api/v1/workspaces` — list workspaces for a supplied current-user identity;
  - `GET /api/v1/workspaces/{workspaceId}` — fetch a workspace visible to that identity.
- Keep current-user extraction behind the application-facing `CurrentUserProvider` port, whose API has no servlet/request types. The temporary `X-User-Id` implementation is a local/test-only adapter, active only under explicit `local` or `test` profiles; it must not be available under the default or production profile. A production deployment must provide a separately implemented trusted authentication adapter before exposing these endpoints. No JWT implementation is part of this slice, and the local adapter must not query `user-service`'s database.
- Add unit and web-layer tests for the initial use cases and validation.
- Update root system context to record that the service exists, its data ownership, local port/database, and the temporary identity boundary.

### Out of scope

- Documents and document permissions.
- Realtime collaboration, WebSocket, Yjs, Redis, or presence.
- Message brokers, outbox processing, and integration events.
- API gateway or deployment orchestration.
- Calling `user-service` synchronously.
- Production-grade JWT key distribution; this requires a separate authentication contract.
- Full invitation HTTP workflows; invitation persistence is prepared for a subsequent slice.

## Architecture

The service uses a conventional Spring MVC layered architecture. Controllers translate HTTP input into DTOs and delegate to application services. Services enforce workspace invariants and use repositories. Repositories are the only layer that knows Spring Data persistence details. Entities contain workspace-service-owned state; user references remain UUID values and are not foreign keys to another service's database.

The first API slice is deliberately small: workspace creation and read access demonstrate the complete controller-to-service-to-repository path. The current-user identity is represented by a local `CurrentUserProvider` boundary; replacing its adapter with JWT validation later must not change the application service signatures.

Under the default/production profile, the local header provider is absent. If no separately implemented trusted `CurrentUserProvider` is supplied, application context startup must fail fast because the controller cannot be wired; the service must never start with anonymous identity or an `X-User-Id` fallback.

## Domain and persistence decisions

- Table names are `workspace`, `workspace_member`, and `workspace_invitation`.
- All entity identifiers are UUIDs generated by the application/JPA layer.
- `workspace_member` has a unique `(workspace_id, user_id)` constraint.
- A created workspace receives an owner membership with role `OWNER` in the same transaction.
- Workspace-name handling is consistent across layers: the request DTO rejects blank values and raw values longer than 100 characters; the application service trims leading/trailing whitespace, rejects an empty or overlong canonical value, and persists only the canonical value; JPA and PostgreSQL enforce non-null `VARCHAR(100)` storage plus a check that the stored value is already trimmed and non-empty.
- Workspace timestamps use UTC `Instant` values.
- `WorkspaceInvitation` remains persistence preparation only. The current draft fields are `id`, `workspace`, `inviteeUserId`, `invitedByUserId`, `status`, `expiresAt`, `createdAt`, and `respondedAt`; its supported source-defined statuses are `PENDING`, `ACCEPTED`, `DECLINED`, and `EXPIRED`, stored as a string enum. The current draft declares no invitation uniqueness or lifecycle constraints. The migration may add only structural integrity for the draft (local workspace foreign key, required identity/status/created-at fields, and the enum value check); `expires_at` and `responded_at` remain nullable. No uniqueness rule, expiry transition, acceptance/decline workflow, or invitation endpoint is introduced by this slice.
- `user_id`, `invitee_user_id`, and `invited_by_user_id` are opaque UUID references owned by `user-service`; no cross-service foreign keys are created.

The workspace repository exposes both membership-filtered collection access and membership-filtered lookup by identifier: `findDistinctByMembers_UserIdOrderByCreatedAtAsc(UUID userId)` and `findByIdAndMembers_UserId(UUID workspaceId, UUID userId)`. Public reads must use the latter rather than loading by workspace ID alone.

## HTTP error contract

All handled errors use the observed `user-service` JSON shape: `timestamp`, `status`, `error`, `message`, `path`, and optional `fields`. This service owns its handler; it does not depend on `user-service` at runtime.

- Invalid or missing `X-User-Id` in local/test mode returns HTTP 401 with `CURRENT_USER_REQUIRED`.
- A malformed workspace path UUID returns HTTP 400 with `INVALID_UUID`.
- Malformed JSON returns HTTP 400 with `MALFORMED_JSON`.
- DTO validation failures return HTTP 400 with `VALIDATION_ERROR` and field errors.
- An application-level canonical-name invariant failure reached through HTTP returns HTTP 400 with `VALIDATION_ERROR` and a `name` field error.
- A missing workspace and a caller without membership both return HTTP 404 with `WORKSPACE_NOT_FOUND`.
- Persistence and infrastructure exceptions are not exposed as raw stack traces.

## Verification

- Maven tests run from `services/workspace-service`.
- Unqualified `mvn test` uses the H2 test profile and does not require PostgreSQL; `WorkspacePostgresMigrationSmokeIT` is run only by an explicit test command.
- Unit and integration tests cover entity invariants, owner-membership creation, transaction rollback when owner-membership persistence fails, DB-enforced membership uniqueness, membership-filtered lookup by workspace ID and user ID, request validation, malformed UUID/JSON, hidden-not-found behavior, and application context startup.
- A PostgreSQL migration smoke check uses the existing Compose PostgreSQL service (`workspace_db`, host port `5433`) but runs against a uniquely named temporary database created inside that service. It drops only that temporary database afterward, stops PostgreSQL only when this check started it, and never removes the named volume or stops a PostgreSQL process that was already running. The smoke class is named `WorkspacePostgresMigrationSmokeIT`, which is outside Surefire's default `*Test` patterns, and is invoked explicitly. H2 is only the isolated default test context and is not evidence that PostgreSQL migrations are valid.

## Documentation impact

Because this creates a service and establishes its responsibility, implementation must update `docs/context/PRODUCT.md`, `docs/context/INDEX.md`, `docs/context/SERVICE-CATALOG.md`, `docs/context/ARCHITECTURE.md`, `docs/context/DATA-OWNERSHIP.md`, `docs/context/COMMUNICATION.md`, and `docs/context/ROADMAP.md`, plus add `services/workspace-service/AGENTS.md` and service context documents. The updates must preserve the open Workspace-versus-Room decision in `docs/context/DECISIONS.md`. The root `AGENTS.md` should no longer claim that only `user-service` exists once the implementation is source-verified.
