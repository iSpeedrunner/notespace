# Workspace Service Layered Structure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved layered foundation and first workspace read/create API for `workspace-service`.

**Architecture:** Use conventional Spring MVC layers: controllers depend on DTOs and application services; application services depend on repository interfaces and a current-user boundary; Spring Data repositories own persistence access; JPA entities model only workspace-service data. User references remain UUIDs and never become cross-service foreign keys.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, PostgreSQL, Flyway, Bean Validation, JUnit 5, Mockito, MockMvc, and H2 only for isolated test-context startup.

**Spec:** `docs/superpowers/specs/2026-10-08-workspace-service-layered-structure-design.md`

## Global Constraints

- Do not read or modify `user-service`'s database.
- Keep `userId`, `inviteeUserId`, and `invitedByUserId` as opaque UUID values without cross-service foreign keys.
- The first API surface is `POST /api/v1/workspaces`, `GET /api/v1/workspaces`, and `GET /api/v1/workspaces/{workspaceId}`.
- Workspace names are required, trimmed, and limited to 100 characters.
- A created workspace and its `OWNER` membership are persisted in one transaction.
- Do not add documents, realtime, Redis, a broker, an outbox, a gateway, or synchronous calls to `user-service`.
- Keep the temporary local identity adapter behind `CurrentUserProvider`; use `X-User-Id` only as a local-development boundary.
- Run Maven commands from `services/workspace-service`.

## Review Focus

- Missing or malformed `X-User-Id` must return a stable 401 error without entering the application service; covered in Task 3 `WorkspaceControllerWebTest.currentUserHeaderIsRequired`.
- A member reading a workspace succeeds while a non-member receives 404; covered in Task 2 `WorkspaceApplicationServiceTest` and Task 3 `WorkspaceControllerWebTest.workspaceIsHiddenFromNonMember`.
- Duplicate workspace membership must be rejected by the database constraint and not modeled as a cross-service relationship; covered in Task 1 `WorkspacePersistenceMappingTest`.
- A blank, whitespace-only, or overlong workspace name must return validation failure; covered in Task 3 `WorkspaceControllerWebTest.invalidWorkspaceNameIsRejected`.
- The application context must start and Flyway must validate against an isolated test database without requiring a developer's PostgreSQL process; covered in Task 1 `WorkspaceServiceApplicationTests`.

### Task 1: Domain and Persistence Foundation

**Files:**
- Modify: `services/workspace-service/pom.xml` — add H2 as a test-only driver.
- Modify: `services/workspace-service/src/main/java/com/notespace/workspace_service/entity/Workspace.java` — complete the entity, accessors, timestamps, and membership relationship.
- Modify: `services/workspace-service/src/main/java/com/notespace/workspace_service/entity/WorkspaceMember.java` — complete UUID generation, explicit join column, enum mapping, and accessors.
- Modify: `services/workspace-service/src/main/java/com/notespace/workspace_service/entity/WorkspaceInvitation.java` — complete UUID generation, explicit join column, enum mapping, and accessors.
- Modify: `services/workspace-service/src/main/java/com/notespace/workspace_service/entity/WorkspaceRole.java` — define `OWNER` and `MEMBER`.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/repository/WorkspaceRepository.java` — workspace persistence and membership-filtered reads.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/repository/WorkspaceMemberRepository.java` — membership persistence and lookup.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/repository/WorkspaceInvitationRepository.java` — invitation persistence seam.
- Create: `services/workspace-service/src/main/resources/db/migration/V1__create_workspace_tables.sql` — PostgreSQL schema and local constraints.
- Create: `services/workspace-service/src/test/resources/application-test.yaml` — H2 test datasource, Flyway, and JPA test settings.
- Modify: `services/workspace-service/src/test/java/com/notespace/workspace_service/WorkspaceServiceApplicationTests.java` — run the context under the test profile.
- Create: `services/workspace-service/src/test/java/com/notespace/workspace_service/WorkspacePersistenceMappingTest.java` — verify role values, unique membership mapping, and migration-compatible columns.

**Interfaces:**
- Produces `WorkspaceRepository extends JpaRepository<Workspace, UUID>` with `List<Workspace> findDistinctByMembers_UserIdOrderByCreatedAtAsc(UUID userId)`.
- Produces `WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID>` with `boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId)`.
- Produces `WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID>`.
- Produces entity accessors required by the service layer: `Workspace.getId()`, `getName()`, `getCreatedAt()`, `getUpdatedAt()` and corresponding member/invitation accessors.

- [ ] **Step 1: Write the failing persistence tests**

  Add `WorkspacePersistenceMappingTest` assertions for the exact enum values `OWNER` and `MEMBER`, the `(workspace_id, user_id)` uniqueness declaration, and the three entity table names. Update `WorkspaceServiceApplicationTests` to activate `test` and expect the application context to load through Flyway/H2.

- [ ] **Step 2: Run the tests to verify they fail**

  Run: `.\mvnw.cmd -Dtest=WorkspacePersistenceMappingTest,WorkspaceServiceApplicationTests test`

  Expected: FAIL because `WorkspaceRole` has no values, the mappings/migration are incomplete, and the test profile/dependency is not present yet.

- [ ] **Step 3: Implement the persistence foundation**

  Add protected no-argument constructors for JPA, UUID generation for all entity IDs, explicit `@JoinColumn` names, `@OneToMany(mappedBy = "workspace")` on `Workspace`, and a static owner-membership factory or constructor that sets `WorkspaceRole.OWNER`. Use `TIMESTAMPTZ`, `VARCHAR(100)`, enum check constraints, cascading workspace-member/invitation foreign keys, and indexes in `V1__create_workspace_tables.sql`. Keep the invitation entity persistence-only; do not add invitation endpoints.

- [ ] **Step 4: Run the persistence tests to verify they pass**

  Run: `.\mvnw.cmd -Dtest=WorkspacePersistenceMappingTest,WorkspaceServiceApplicationTests test`

  Expected: PASS with the application context loading against the H2 test profile and the mapping assertions green.

- [ ] **Step 5: Commit the persistence foundation**

  ```bash
  git add services/workspace-service/pom.xml services/workspace-service/src/main/java/com/notespace/workspace_service/entity services/workspace-service/src/main/java/com/notespace/workspace_service/repository services/workspace-service/src/main/resources/db/migration/V1__create_workspace_tables.sql services/workspace-service/src/test
  git commit -m "feat: add workspace persistence foundation"
  ```

### Task 2: Application Services and Current-User Boundary

**Files:**
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/service/CreateWorkspaceCommand.java` — immutable application input.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/service/WorkspaceView.java` — immutable application output.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/service/WorkspaceApplicationService.java` — transactional create and membership-filtered read use cases.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/service/CurrentUserProvider.java` — request identity boundary.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/exception/WorkspaceNotFoundException.java` — hidden-not-found domain error.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/exception/InvalidCurrentUserException.java` — missing/invalid local identity error.
- Create: `services/workspace-service/src/test/java/com/notespace/workspace_service/service/WorkspaceApplicationServiceTest.java` — unit tests with mocked repositories.

**Interfaces:**
- Consumes `WorkspaceRepository.findDistinctByMembers_UserIdOrderByCreatedAtAsc(UUID)` and `WorkspaceMemberRepository.save(WorkspaceMember)` from Task 1.
- Produces `record CreateWorkspaceCommand(String name, UUID ownerUserId)`.
- Produces `record WorkspaceView(UUID id, String name, Instant createdAt, Instant updatedAt)`.
- Produces `WorkspaceApplicationService.create(CreateWorkspaceCommand command): WorkspaceView`.
- Produces `WorkspaceApplicationService.listForUser(UUID userId): List<WorkspaceView>`.
- Produces `WorkspaceApplicationService.getForUser(UUID userId, UUID workspaceId): WorkspaceView`.
- Produces `CurrentUserProvider.currentUserId(HttpServletRequest request): UUID`.

- [ ] **Step 1: Write the failing application-service tests**

  Add tests that assert `create` trims the name, saves one workspace and one owner membership in the same use case, `listForUser` maps repository results, `getForUser` returns a member-visible workspace, and missing/non-member reads throw `WorkspaceNotFoundException`.

- [ ] **Step 2: Run the tests to verify they fail**

  Run: `.\mvnw.cmd -Dtest=WorkspaceApplicationServiceTest test`

  Expected: FAIL because the command, view, service, exceptions, and repository methods do not exist.

- [ ] **Step 3: Implement the application layer**

  Make `create` transactional, normalize the already-validated name with `trim()`, create UTC timestamps, save the workspace, then save its `OWNER` membership. Make reads use membership-filtered repository access; never load a workspace by ID alone for the public read use case. Map entities to `WorkspaceView` without exposing JPA entities to controllers.

- [ ] **Step 4: Run the application-service tests to verify they pass**

  Run: `.\mvnw.cmd -Dtest=WorkspaceApplicationServiceTest test`

  Expected: PASS with all owner-membership, trimming, mapping, and hidden-not-found assertions green.

- [ ] **Step 5: Commit the application layer**

  ```bash
  git add services/workspace-service/src/main/java/com/notespace/workspace_service/service services/workspace-service/src/main/java/com/notespace/workspace_service/exception services/workspace-service/src/test/java/com/notespace/workspace_service/service
  git commit -m "feat: add workspace application services"
  ```

### Task 3: HTTP API, Local Identity Adapter, and Error Mapping

**Files:**
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/dto/error/ErrorResponse.java` — service-local standard error body.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/dto/workspace/CreateWorkspaceRequest.java` — validated request DTO.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/dto/workspace/WorkspaceResponse.java` — response DTO.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/controller/WorkspaceController.java` — `/api/v1/workspaces` adapter.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/controller/GlobalExceptionHandler.java` — validation, identity, not-found, and fallback mapping.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/config/HeaderCurrentUserProvider.java` — `X-User-Id` adapter.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/config/SecurityConfig.java` — disable CSRF for this stateless temporary adapter and permit MVC requests to reach header identity extraction.
- Create: `services/workspace-service/src/test/java/com/notespace/workspace_service/controller/WorkspaceControllerWebTest.java` — MockMvc/web-layer coverage.

**Interfaces:**
- Consumes `WorkspaceApplicationService` and `CurrentUserProvider` from Task 2.
- Produces `CreateWorkspaceRequest(String name)` with `@NotBlank` and `@Size(max = 100)`.
- Produces `WorkspaceResponse(UUID id, String name, Instant createdAt, Instant updatedAt)`.
- Produces `POST /api/v1/workspaces` → 201 with `WorkspaceResponse`.
- Produces `GET /api/v1/workspaces` → 200 with `List<WorkspaceResponse>`.
- Produces `GET /api/v1/workspaces/{workspaceId}` → 200 with `WorkspaceResponse`.
- Produces 400 `VALIDATION_ERROR`, 401 `CURRENT_USER_REQUIRED`, and 404 `WORKSPACE_NOT_FOUND` responses in the `timestamp/status/error/message/path/fields` shape.

- [ ] **Step 1: Write the failing web-layer tests**

  Add MockMvc tests for create/list/get delegation and response shape, invalid names returning 400, missing or malformed `X-User-Id` returning 401, not-found mapping returning 404, and a non-member read remaining 404.

- [ ] **Step 2: Run the tests to verify they fail**

  Run: `.\mvnw.cmd -Dtest=WorkspaceControllerWebTest test`

  Expected: FAIL because the controller, DTOs, identity provider, security configuration, and exception handler do not exist.

- [ ] **Step 3: Implement the HTTP layer**

  Parse `X-User-Id` with `UUID.fromString`, throw `InvalidCurrentUserException` for absence or malformed values, validate request DTOs with `@Valid`, map application views to response DTOs, and preserve the same 404 for missing and unauthorized workspace reads. Keep the temporary provider local and document that it is not a production authentication mechanism.

- [ ] **Step 4: Run the web-layer tests to verify they pass**

  Run: `.\mvnw.cmd -Dtest=WorkspaceControllerWebTest test`

  Expected: PASS with all status codes, error codes, header parsing, and response mappings green.

- [ ] **Step 5: Commit the HTTP layer**

  ```bash
  git add services/workspace-service/src/main/java/com/notespace/workspace_service/controller services/workspace-service/src/main/java/com/notespace/workspace_service/config services/workspace-service/src/main/java/com/notespace/workspace_service/dto services/workspace-service/src/test/java/com/notespace/workspace_service/controller
  git commit -m "feat: expose workspace lifecycle api"
  ```

### Task 4: Service Guidance, System Context, and Full Verification

**Files:**
- Create: `services/workspace-service/AGENTS.md` — service-local rules and context routing.
- Create: `services/workspace-service/README.md` — local run/test instructions, API examples, and temporary identity note.
- Create: `services/workspace-service/docs/context/INDEX.md` — service context index.
- Create: `services/workspace-service/docs/context/SERVICE.md` — responsibility and boundaries.
- Create: `services/workspace-service/docs/context/ARCHITECTURE.md` — layered topology and dependencies.
- Create: `services/workspace-service/docs/context/DOMAIN.md` — workspace/member/invitation invariants.
- Create: `services/workspace-service/docs/context/DATABASE.md` — owned schema and migration mapping.
- Create: `services/workspace-service/docs/context/API.md` — verified endpoint contract and local identity behavior.
- Modify: `AGENTS.md` — current repository/service state and workspace-service routing.
- Modify: `docs/context/ARCHITECTURE.md` — source-verified topology.
- Modify: `docs/context/SERVICE-CATALOG.md` — add implemented workspace-service entry.
- Modify: `docs/context/DATA-OWNERSHIP.md` — assign workspace-service-owned data.
- Modify: `docs/context/COMMUNICATION.md` — record that no inter-service call or event exists and explain the temporary identity boundary.
- Modify: `docs/context/ROADMAP.md` — mark only the implemented workspace slice as source-verified while leaving invitations/documents/realtime planned.

**Interfaces:**
- Consumes the verified endpoints and persistence behavior from Tasks 1–3.
- Produces documentation that does not claim JWT, invitation workflows, documents, realtime, or events are implemented.

- [ ] **Step 1: Write documentation verification checks**

  Add a checklist to the service README and context docs for `POST`, both `GET` endpoints, `X-User-Id`, port `8082`, database `workspace_db`, and the explicitly deferred capabilities. No product-code test is required for prose-only files.

- [ ] **Step 2: Run documentation consistency checks**

  Run: `rg -n "workspace-service|workspace_db|8082|X-User-Id|JWT|invitation|realtime|broker" AGENTS.md docs/context services/workspace-service/AGENTS.md services/workspace-service/README.md services/workspace-service/docs/context`

  Expected: the new service is listed as implemented only for the delivered slice; JWT production auth, invitation workflows, documents, realtime, and broker remain labeled deferred/planned.

- [ ] **Step 3: Run the complete test suite**

  Run: `.\mvnw.cmd test`

  Expected: PASS with all unit, web, persistence, and context tests green. If PostgreSQL is available, Flyway applies `V1__create_workspace_tables.sql`; otherwise the H2 test profile keeps automated tests independent of the local database process.

- [ ] **Step 4: Inspect the final diff**

  Run: `git diff --check HEAD~4..HEAD; git status --short; git diff --stat HEAD~4..HEAD`

  Expected: no whitespace errors; only workspace-service implementation/context files and intended root context changes are present; pre-existing `.idea/` remains untouched.

- [ ] **Step 5: Commit documentation and verification updates**

  ```bash
  git add -- AGENTS.md docs/context services/workspace-service/AGENTS.md services/workspace-service/README.md services/workspace-service/docs/context
  git commit -m "docs: document workspace service boundaries"
  ```
