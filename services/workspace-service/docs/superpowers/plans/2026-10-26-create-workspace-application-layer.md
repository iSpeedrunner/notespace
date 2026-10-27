# Create Workspace Application Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement an atomic application-layer use case that creates a normalized workspace and its owner membership.

**Architecture:** `WorkspaceApplicationService` remains the application boundary and uses the existing Spring Data repositories. It validates inputs before persistence, saves the aggregate's initial records within a Spring transaction, and keeps future authentication behind an unused `CurrentUserProvider` interface.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, Spring transaction management, JUnit 5, H2 test database, Mockito bean override.

**Spec:** `docs/superpowers/specs/2026-10-26-create-workspace-application-layer-design.md`

## Global Constraints

- Do not add a REST controller, JWT support, user-service integration, or infrastructure.
- `create` accepts only trusted application-layer inputs: `String name` and `UUID ownerUserId`.
- Names are trimmed, non-blank, and at most 100 characters.
- Workspace and initial `OWNER` membership must share one transaction.
- `CurrentUserProvider` is an interface only in this change.

## Review Focus

- A name consisting solely of whitespace must fail before either repository is written.
- A name longer than 100 characters must fail before either repository is written.
- A null owner ID must fail before either repository is written.
- A membership-write runtime failure must leave no committed workspace record.
- Workspace creation must set `createdAt`, `updatedAt`, and owner `joinedAt` rather than persisting null timestamps.

---

### Task 1: Establish application-layer contracts and successful creation behavior

**Files:**
- Create: `src/main/java/com/notespace/workspace_service/service/CurrentUserProvider.java`
- Modify: `src/main/java/com/notespace/workspace_service/service/WorkspaceApplicationService.java`
- Create: `src/test/java/com/notespace/workspace_service/service/WorkspaceApplicationServiceIntegrationTest.java`

**Interfaces:**
- Consumes: `WorkspaceRepository.save(Workspace)` and `WorkspaceMemberRepository.save(WorkspaceMember)`.
- Produces: `Workspace create(String name, UUID ownerUserId)` and `UUID CurrentUserProvider.getCurrentUserId()`.

- [ ] **Step 1: Write the failing successful-creation integration test**

Create a Spring integration test that calls `create("  Product Team  ", ownerUserId)` and asserts a persisted workspace named `"Product Team"`, non-null timestamps, and one persisted membership with the same workspace ID, owner ID, `OWNER` role, and non-null `joinedAt`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw test -Dtest=WorkspaceApplicationServiceIntegrationTest#createsNormalizedWorkspaceAndOwnerMembership`

Expected: FAIL because `WorkspaceApplicationService#create` is absent.

- [ ] **Step 3: Implement `Workspace create(String name, UUID ownerUserId)` and `CurrentUserProvider`**

Inject the two repositories through a constructor. Mark `create` with `@Transactional`, reject null or invalid input with `IllegalArgumentException`, save a workspace with the normalized name and one current instant for its timestamps, then save its `OWNER` member and return the saved workspace. Declare `CurrentUserProvider#getCurrentUserId()` without any implementation or caller.

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `./mvnw test -Dtest=WorkspaceApplicationServiceIntegrationTest#createsNormalizedWorkspaceAndOwnerMembership`

Expected: PASS.

### Task 2: Protect input invariants and transactional rollback

**Files:**
- Modify: `src/test/java/com/notespace/workspace_service/service/WorkspaceApplicationServiceIntegrationTest.java`
- Create: `src/test/java/com/notespace/workspace_service/service/WorkspaceApplicationServiceRollbackIntegrationTest.java`

**Interfaces:**
- Consumes: `WorkspaceApplicationService#create(String, UUID)` from Task 1.
- Produces: integration-level proof that invalid input makes no writes and that a membership repository failure rolls back workspace persistence.

- [ ] **Step 1: Write failing input-validation tests**

Add tests calling `create` with whitespace-only name, a 101-character name, and null owner ID. Assert `IllegalArgumentException` and no records in either real repository.

- [ ] **Step 2: Run the input-validation tests to verify they fail**

Run: `./mvnw test -Dtest=WorkspaceApplicationServiceIntegrationTest`

Expected: FAIL until validation covers each invalid input.

- [ ] **Step 3: Write the failing rollback integration test**

Create a separate Spring test context with the membership repository overridden by a Mockito bean that throws `DataIntegrityViolationException` from `save`. Call `create` with valid arguments, assert that exception, and assert the real workspace repository has no records after the transaction completes.

- [ ] **Step 4: Run the rollback test to verify it fails**

Run: `./mvnw test -Dtest=WorkspaceApplicationServiceRollbackIntegrationTest`

Expected: FAIL until `create` is transactional and membership persistence is within that transaction.

- [ ] **Step 5: Complete the smallest validation and transaction changes needed for both tests**

Keep validation before the first repository call and retain the runtime membership exception so Spring marks the transaction for rollback.

- [ ] **Step 6: Run targeted tests and the complete suite**

Run: `./mvnw test -Dtest=WorkspaceApplicationServiceIntegrationTest,WorkspaceApplicationServiceRollbackIntegrationTest`

Expected: PASS.

Run: `./mvnw test`

Expected: PASS with no test failures.
