# Create Workspace Application Layer Design

## Intent

Provide the first workspace lifecycle use case without exposing an HTTP API or coupling the service to a particular authentication mechanism. A caller supplies the workspace name and the trusted owner identity; the application service creates the workspace and its initial owner membership atomically.

## Goals

- Provide `WorkspaceApplicationService#create(String name, UUID ownerUserId)`.
- Normalize a supplied name with `trim`, reject blank names, and enforce the database's 100-character limit.
- Persist a `Workspace` and an initial `WorkspaceMember` with `OWNER` role in one transaction.
- Roll back workspace persistence when membership persistence fails.
- Introduce `CurrentUserProvider` as an authentication-mechanism-independent interface only.

## Non-goals

- REST endpoints, request/response DTOs, headers, JWT processing, or user-service calls.
- A `CurrentUserProvider` implementation or use of it in this use case.
- New infrastructure or cross-service integration.

## Constraints

- The workspace service owns workspace and membership persistence only.
- `ownerUserId` is an application-layer argument for now; a future production HTTP boundary must derive it from a trusted identity contract, never a client-controlled field.
- `Workspace` and its owner membership must be committed or rolled back together.

## Approaches considered

1. **Selected — explicit owner argument with deferred identity adapter.** Keep the requested `create(name, ownerUserId)` signature and declare `CurrentUserProvider` independently. It implements the required use case without inventing an authentication integration.
2. Add `create(name)` now and require a `CurrentUserProvider` bean. Rejected because no authentication mechanism or provider implementation exists yet.

## Design

`WorkspaceApplicationService` receives `WorkspaceRepository` and `WorkspaceMemberRepository` through constructor injection. Its transactional `create` method normalizes and validates the name, obtains one `Instant` for both workspace timestamps, saves the workspace, then saves its owner membership with `joinedAt` set to that instant. It returns the created `Workspace`.

The service rejects `null` owner IDs and invalid names with `IllegalArgumentException` before persistence. The membership persistence failure remains a runtime exception, so Spring rolls back the surrounding transaction.

`CurrentUserProvider` declares `UUID getCurrentUserId()` and has no implementation or consumer in this change.

## Verification

- An integration test proves that a trimmed valid name creates a persisted workspace and an `OWNER` membership for the provided user ID.
- A transactional integration test replaces the membership repository with a test double that throws after the workspace save and proves the workspace repository remains empty after `create` fails.
