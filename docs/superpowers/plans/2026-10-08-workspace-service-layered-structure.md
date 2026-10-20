# Workspace Service Layered Structure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved layered foundation and first workspace read/create API for `workspace-service`.

**Architecture:** Use conventional Spring MVC layers: controllers depend on DTOs and application services; application services depend on repository interfaces and application-facing ports; Spring Data repositories own persistence access; JPA entities model only workspace-service data. User references remain UUIDs and never become cross-service foreign keys. HTTP/request access is isolated in adapters, not in application ports or use cases.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, PostgreSQL, Flyway, Bean Validation, JUnit 5, Mockito, MockMvc, and H2 only for isolated test-context startup.

**Spec:** `docs/superpowers/specs/2026-10-08-workspace-service-layered-structure-design.md`

**Execution ledger preflight (from repository root, before Task 1):** Create the plan ledger, capture the exact current commit, and write it before any task changes:

```powershell
$ledgerPath = '.superpowers/sdd/2026-10-08-workspace-service-layered-structure/progress.md'
New-Item -ItemType Directory -Force (Split-Path $ledgerPath) | Out-Null
$baseCommit = (git rev-parse HEAD).Trim()
if ($baseCommit -notmatch '^[0-9a-f]{40}$') { throw 'Could not capture exact BASE_COMMIT' }
Set-Content -LiteralPath $ledgerPath -Value '# SDD ledger — plan: 2026-10-08-workspace-service-layered-structure'
Add-Content -LiteralPath $ledgerPath -Value "BASE_COMMIT: $baseCommit"
```

The ledger is execution bookkeeping, not application code. Do not derive the baseline later from a commit message or parent count.

## Global Constraints

- Do not read or modify `user-service`'s database.
- Keep `userId`, `inviteeUserId`, and `invitedByUserId` as opaque UUID values without cross-service foreign keys.
- The first API surface is `POST /api/v1/workspaces`, `GET /api/v1/workspaces`, and `GET /api/v1/workspaces/{workspaceId}`.
- Workspace names are rejected as blank by DTO validation, trimmed and revalidated by the application service, persisted only in canonical trimmed form, and constrained by JPA/PostgreSQL to non-empty `VARCHAR(100)` values.
- A created workspace and its `OWNER` membership are persisted in one transaction.
- `WorkspaceRepository` must expose membership-filtered collection and ID lookup methods; public reads must never load a workspace by ID alone.
- `CurrentUserProvider` is an application-facing port with no servlet/request parameter. The repository has no pre-existing profile naming convention, so this plan uses only the canonical profile names `local`, `test`, `postgres`, and `production`. The `X-User-Id` implementation is available only under `local & !production` or `test & !production`; production requires a separately implemented trusted authentication adapter and has no header fallback. The PostgreSQL smoke context activates only `postgres` and supplies a test-only `CurrentUserProvider` mock; it never activates the local header adapter and never requires production JWT authentication.
- The current `WorkspaceInvitation` draft contains `id`, `workspace`, `inviteeUserId`, `invitedByUserId`, `status`, `expiresAt`, `createdAt`, and `respondedAt`; its status is limited to `PENDING`, `ACCEPTED`, `DECLINED`, and `EXPIRED`. The migration may add only structural integrity for those fields; no invitation uniqueness rule or lifecycle workflow is introduced.
- Do not add documents, realtime, Redis, a broker, an outbox, a gateway, or synchronous calls to `user-service`.
- Run Maven commands from `services/workspace-service`.
- Run Git commands from the repository root (`D:\projects\notespace`).

## Review Focus

- Missing or malformed `X-User-Id` must return a stable 401 error without entering the application service; covered in Task 3 `WorkspaceControllerWebTest.currentUserHeaderIsRequired`.
- A member reading a workspace succeeds while a non-member receives 404; covered in Task 2 `WorkspaceApplicationServiceTest` and Task 3 `WorkspaceControllerWebTest.workspaceIsHiddenFromNonMember`.
- Duplicate workspace membership must be rejected by the real database unique constraint; covered in Task 1 `WorkspacePersistenceIntegrationTest.duplicateMembershipIsRejectedByDatabaseConstraint`.
- A blank, whitespace-only, or overlong workspace name must return validation failure, while surrounding whitespace on a valid name is removed before persistence; covered in Task 2 and Task 3 name tests.
- A malformed path UUID returns 400 `INVALID_UUID`, and malformed JSON returns 400 `MALFORMED_JSON`; covered in Task 3 `WorkspaceControllerWebTest`.
- Owner-membership persistence failure rolls back the workspace insert; covered by Task 2 `WorkspaceTransactionIntegrationTest.createWorkspaceRollsBackWhenOwnerMembershipInsertFails` against a real test database.
- The PostgreSQL Flyway migration and Hibernate schema validation run against an isolated temporary database in the existing Compose PostgreSQL service; covered by Task 1 `WorkspacePostgresMigrationSmokeIT`.
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
- Create: `services/workspace-service/src/test/resources/application-postgres.yaml` — explicit local PostgreSQL smoke-test settings using `WORKSPACE_POSTGRES_TEST_URL`, username `workspace_service`, password `password`, and `spring.jpa.hibernate.ddl-auto: validate`; it targets Compose service `postgres`, database `workspace_db`, host port `5433`.
- Modify: `services/workspace-service/src/test/java/com/notespace/workspace_service/WorkspaceServiceApplicationTests.java` — run the context under the test profile.
- Create: `services/workspace-service/src/test/java/com/notespace/workspace_service/WorkspacePersistenceIntegrationTest.java` — verify entity mappings and DB-enforced membership uniqueness.
- Create: `services/workspace-service/src/test/java/com/notespace/workspace_service/WorkspacePostgresMigrationSmokeIT.java` — explicit-only full `@SpringBootTest` migration/Hibernate smoke test under `@ActiveProfiles("postgres")`; it supplies a test-only `@MockitoBean CurrentUserProvider`, asserts that `LocalHeaderCurrentUserProvider` is absent, and the `*IT` suffix keeps it out of Maven Surefire's default `mvn test` patterns.

**Interfaces:**
- Produces `WorkspaceRepository extends JpaRepository<Workspace, UUID>` with `List<Workspace> findDistinctByMembers_UserIdOrderByCreatedAtAsc(UUID userId)`.
- Produces `WorkspaceRepository` with `Optional<Workspace> findByIdAndMembers_UserId(UUID workspaceId, UUID userId)` for authorization-safe lookup.
- Produces `WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID>` with `boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId)`.
- Produces `WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID>`.
- Produces entity accessors required by the service layer: `Workspace.getId()`, `getName()`, `getCreatedAt()`, `getUpdatedAt()` and corresponding member/invitation accessors.
- Retains the existing production dependencies in `pom.xml`: Spring Security, MVC, Validation, JPA, Flyway, PostgreSQL runtime, and their existing test starters. Add only the H2 test driver if needed; do not add duplicate starters or duplicate database drivers.

- [ ] **Step 1: Write the failing persistence tests**

  Add `WorkspacePersistenceIntegrationTest` using the H2 test profile to persist two members with the same `(workspace_id, user_id)`, call `saveAndFlush()` for the conflicting member, and assert the database constraint exception. Verify the exact enum values `OWNER` and `MEMBER` and the three entity table names. Update `WorkspaceServiceApplicationTests` to activate `test` and expect the application context to load through Flyway/H2. Add `WorkspacePostgresMigrationSmokeIT` as a full `@SpringBootTest` with `@ActiveProfiles("postgres")` and a test-only `@MockitoBean CurrentUserProvider`; assert that no `LocalHeaderCurrentUserProvider` bean is registered. The smoke context must not activate `local` or `test`, must not require production JWT authentication, and must not be selected by an unqualified `mvn test`.

- [ ] **Step 2: Run the tests to verify they fail**

  Run: `.\mvnw.cmd -Dtest=WorkspacePersistenceIntegrationTest,WorkspaceServiceApplicationTests test`

  Expected: FAIL because `WorkspaceRole` has no values, the mappings/migration/repositories are incomplete, the `saveAndFlush()` constraint test has no implementation to exercise, and the test profiles/tests are not present yet.

- [ ] **Step 3: Implement the persistence foundation**

  Add protected no-argument constructors for JPA, UUID generation for all entity IDs, explicit `@JoinColumn` names, `@OneToMany(mappedBy = "workspace")` on `Workspace`, and a static owner-membership factory or constructor that sets `WorkspaceRole.OWNER`. Use `TIMESTAMPTZ`, `VARCHAR(100)`, trimmed/non-empty workspace-name checks, the `OWNER`/`MEMBER` role check, the four source-defined invitation-status values, local workspace foreign keys, the `(workspace_id, user_id)` unique constraint, and indexes in `V1__create_workspace_tables.sql`. For invitations, preserve the draft fields and add only structural integrity: required identity/status/created-at columns, the workspace FK, and the status check; keep `expires_at` and `responded_at` nullable. Do not add invitation uniqueness, expiry transitions, or invitation endpoints. Set `application-postgres.yaml` to read `spring.datasource.url` from `${WORKSPACE_POSTGRES_TEST_URL}`, keep username `workspace_service` and password `password`, enable Flyway, and set `spring.jpa.hibernate.ddl-auto: validate`. Annotate the PostgreSQL smoke test with the `postgres` profile and name it `WorkspacePostgresMigrationSmokeIT` so it is never part of the default H2/Surefire suite; because it loads the full application context, provide its `CurrentUserProvider` with `@MockitoBean` (or an equivalent test-only bean) and assert the local header provider is absent. Audit `pom.xml` before editing it: retain the existing Spring Security, MVC, Validation, Flyway, PostgreSQL, JPA, and test starter dependencies; add only H2 test scope if needed.

- [ ] **Step 4: Run the persistence tests to verify they pass**

  Run: `.\mvnw.cmd -Dtest=WorkspacePersistenceIntegrationTest,WorkspaceServiceApplicationTests test`

  Expected: PASS with the application context loading against H2 and the DB uniqueness assertion green. The PostgreSQL smoke test runs explicitly in Task 4 against Compose.

- [ ] **Step 5: Commit the persistence foundation**

  ```bash
  git add -- services/workspace-service/pom.xml services/workspace-service/src/main/java/com/notespace/workspace_service/entity services/workspace-service/src/main/java/com/notespace/workspace_service/repository services/workspace-service/src/main/resources/db/migration/V1__create_workspace_tables.sql services/workspace-service/src/test
  git commit -m "feat: add workspace persistence foundation"
  ```

### Task 2: Application Services and Current-User Boundary

**Files:**
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/service/CreateWorkspaceCommand.java` — immutable application input.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/service/WorkspaceView.java` — immutable application output.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/service/WorkspaceApplicationService.java` — transactional create and membership-filtered read use cases.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/service/CurrentUserProvider.java` — request identity boundary.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/exception/WorkspaceNotFoundException.java` — hidden-not-found domain error.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/exception/InvalidWorkspaceNameException.java` — application invariant error for direct callers.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/exception/InvalidCurrentUserException.java` — missing/invalid local identity error.
- Create: `services/workspace-service/src/test/java/com/notespace/workspace_service/service/WorkspaceApplicationServiceTest.java` — unit tests with mocked repositories.
- Create: `services/workspace-service/src/test/java/com/notespace/workspace_service/service/WorkspaceTransactionIntegrationTest.java` — real transaction rollback test against the test database.

**Interfaces:**
- Consumes `WorkspaceRepository.findDistinctByMembers_UserIdOrderByCreatedAtAsc(UUID)`, `WorkspaceRepository.findByIdAndMembers_UserId(UUID, UUID)`, and `WorkspaceMemberRepository.save(WorkspaceMember)` from Task 1.
- Produces `record CreateWorkspaceCommand(String name, UUID ownerUserId)`.
- Produces `record WorkspaceView(UUID id, String name, Instant createdAt, Instant updatedAt)`.
- Produces `WorkspaceApplicationService.create(CreateWorkspaceCommand command): WorkspaceView`.
- Produces `WorkspaceApplicationService.listForUser(UUID userId): List<WorkspaceView>`.
- Produces `WorkspaceApplicationService.getForUser(UUID userId, UUID workspaceId): WorkspaceView`.
- Produces application-facing `CurrentUserProvider.currentUserId(): UUID`; this port has no `HttpServletRequest`, servlet, or Spring MVC dependency.

- [ ] **Step 1: Write the failing application-service tests**

  Add tests that assert `create` trims the name, rejects a blank or overlong canonical name when called directly, saves one workspace and one owner membership in the same use case, `listForUser` maps repository results, `getForUser` delegates to `findByIdAndMembers_UserId` and returns a member-visible workspace, and missing/non-member reads throw `WorkspaceNotFoundException`. Add `WorkspaceTransactionIntegrationTest` as a non-transactional test method that autowires the real `WorkspaceApplicationService` bean from the Spring context, verifies it is proxied by Spring transaction infrastructure, uses a test-only repository spy/decorator that delegates the real member insert and then throws, and reads workspace/member counts only after the service call has returned/rolled back. The assertions must prove both rows are absent.

- [ ] **Step 2: Run the tests to verify they fail**

  Run: `.\mvnw.cmd -Dtest=WorkspaceApplicationServiceTest,WorkspaceTransactionIntegrationTest test`

  Expected: FAIL because the command, view, service, exceptions, and repository methods do not exist.

- [ ] **Step 3: Implement the application layer**

  Make `create` transactional, canonicalize with `trim()`, reject an empty or over-100-character canonical value, create UTC timestamps, save the workspace, then save its `OWNER` membership. Make `listForUser` use the membership-filtered collection method and `getForUser` use `findByIdAndMembers_UserId`; never load a workspace by ID alone for the public read use case. Map entities to `WorkspaceView` without exposing JPA entities to controllers. Keep `CurrentUserProvider` as a port only; the application service receives UUIDs in commands/method arguments and never reads HTTP state.

- [ ] **Step 4: Run the application-service tests to verify they pass**

  Run: `.\mvnw.cmd -Dtest=WorkspaceApplicationServiceTest,WorkspaceTransactionIntegrationTest test`

  Expected: PASS with owner-membership, canonical-name, mapping, membership-filtered lookup, and hidden-not-found assertions green; the integration test must demonstrate rollback through the real Spring transactional proxy and post-transaction DB reads.

- [ ] **Step 5: Commit the application layer**

  ```bash
  git add -- services/workspace-service/src/main/java/com/notespace/workspace_service/service services/workspace-service/src/main/java/com/notespace/workspace_service/exception services/workspace-service/src/test/java/com/notespace/workspace_service/service
  git commit -m "feat: add workspace application services"
  ```

### Task 3: HTTP API, Local Identity Adapter, and Error Mapping

**Files:**
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/dto/error/ErrorResponse.java` — service-local standard error body.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/dto/workspace/CreateWorkspaceRequest.java` — validated request DTO.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/dto/workspace/WorkspaceResponse.java` — response DTO.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/controller/WorkspaceController.java` — `/api/v1/workspaces` adapter.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/controller/GlobalExceptionHandler.java` — validation, identity, UUID, JSON, not-found, and fallback mapping.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/config/LocalHeaderCurrentUserProvider.java` — local/test-only `X-User-Id` adapter, active under `local & !production` or `test & !production` only.
- Create: `services/workspace-service/src/main/java/com/notespace/workspace_service/config/SecurityConfig.java` — local/test-only MVC security configuration; no production header fallback.
- Create: `services/workspace-service/src/test/java/com/notespace/workspace_service/controller/WorkspaceControllerWebTest.java` — MockMvc/web-layer coverage.
- Create: `services/workspace-service/src/test/java/com/notespace/workspace_service/config/WorkspaceProfileSafetyTest.java` — prove the local header provider is present only for `local` or `test` without `production`, absent for `postgres` and for `production` combined with either local/test, and default/production application wiring fails fast without a trusted `CurrentUserProvider`.

**Interfaces:**
- Consumes `WorkspaceApplicationService` and `CurrentUserProvider` from Task 2.
- Produces `CreateWorkspaceRequest(String name)` with `@NotBlank` and `@Size(max = 100)`.
- Produces `WorkspaceResponse(UUID id, String name, Instant createdAt, Instant updatedAt)`.
- Produces `LocalHeaderCurrentUserProvider` under the explicit profile expressions `local & !production` or `test & !production` only; it implements `CurrentUserProvider.currentUserId()` without changing the application port to an HTTP type. Its servlet/request access is isolated inside the config adapter. The `postgres` profile never enables this adapter.
- Produces `POST /api/v1/workspaces` → 201 with `WorkspaceResponse`.
- Produces `GET /api/v1/workspaces` → 200 with `List<WorkspaceResponse>`.
- Produces `GET /api/v1/workspaces/{workspaceId}` → 200 with `WorkspaceResponse`.
- Produces 400 `VALIDATION_ERROR`, 400 `INVALID_UUID`, 400 `MALFORMED_JSON`, 401 `CURRENT_USER_REQUIRED`, and 404 `WORKSPACE_NOT_FOUND` responses in the `timestamp/status/error/message/path/fields` shape.
- Production/default profiles must not register `LocalHeaderCurrentUserProvider`; a future production deployment must provide a separately implemented trusted authentication provider before these endpoints are exposed.
- Default/production context behavior is fail-fast: because the controller requires `CurrentUserProvider` and no local header provider is active, startup must fail until a separately implemented trusted provider is supplied. There is no anonymous or header-based fallback.

- [ ] **Step 1: Write the failing web-layer tests**

  Add MockMvc tests for create/list/get delegation and response shape, blank/whitespace-only/overlong names returning 400, valid surrounding whitespace being passed to the application service for canonicalization, missing or malformed `X-User-Id` returning 401 without service invocation, malformed path UUID returning 400 `INVALID_UUID`, malformed JSON returning 400 `MALFORMED_JSON`, not-found mapping returning 404, and a non-member read remaining 404. Add `WorkspaceProfileSafetyTest` covering `local`, `test`, `postgres`, `production + local`, and `production + test`: the header provider may exist only in the first two cases; default/production wiring must fail fast without a trusted provider.

- [ ] **Step 2: Run the tests to verify they fail**

  Run: `.\mvnw.cmd -Dtest=WorkspaceControllerWebTest,WorkspaceProfileSafetyTest test`

  Expected: FAIL because the controller, DTOs, identity provider, security configuration, and exception handler do not exist.

- [ ] **Step 3: Implement the HTTP layer**

  Keep DTO validation syntactic (`@NotBlank`, raw `@Size(max = 100)`); pass the value to the application service, which owns canonical trimming and invariant validation. Map `InvalidWorkspaceNameException` to 400 `VALIDATION_ERROR` with a `name` field error. Parse `X-User-Id` with `UUID.fromString` inside the local/test-only config adapter, throw `InvalidCurrentUserException` for absence or malformed values, map `MethodArgumentTypeMismatchException` to `INVALID_UUID`, map `HttpMessageNotReadableException` to `MALFORMED_JSON`, validate request DTOs with `@Valid`, map application views to response DTOs, and preserve the same 404 for missing and unauthorized workspace reads. Annotate both `LocalHeaderCurrentUserProvider` and the local `SecurityConfig` with `@Profile({"local & !production", "test & !production"})`; do not register either in the default, `postgres`, or production profile.

- [ ] **Step 4: Run the web-layer tests to verify they pass**

  Run: `.\mvnw.cmd -Dtest=WorkspaceControllerWebTest,WorkspaceProfileSafetyTest test`

  Expected: PASS with all status codes, error codes, profile restrictions, header parsing, malformed UUID/JSON handling, and response mappings green.

- [ ] **Step 5: Commit the HTTP layer**

  ```bash
  git add -- services/workspace-service/src/main/java/com/notespace/workspace_service/controller services/workspace-service/src/main/java/com/notespace/workspace_service/config services/workspace-service/src/main/java/com/notespace/workspace_service/dto services/workspace-service/src/test/java/com/notespace/workspace_service/controller services/workspace-service/src/test/java/com/notespace/workspace_service/config
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
- Modify: `docs/context/PRODUCT.md` — source-verified status for the delivered Workspace slice while preserving the open Room terminology question.
- Modify: `docs/context/INDEX.md` — route workspace-service architecture/data/API context.
- Modify: `docs/context/ARCHITECTURE.md` — source-verified topology.
- Modify: `docs/context/SERVICE-CATALOG.md` — add implemented workspace-service entry.
- Modify: `docs/context/DATA-OWNERSHIP.md` — assign workspace-service-owned data.
- Modify: `docs/context/COMMUNICATION.md` — record that no inter-service call or event exists and explain the temporary identity boundary.
- Modify: `docs/context/ROADMAP.md` — mark only the implemented workspace slice as source-verified while leaving invitations/documents/realtime planned.

**Interfaces:**
- Consumes the verified endpoints and persistence behavior from Tasks 1–3.
- Produces documentation that does not claim JWT, invitation workflows, documents, realtime, or events are implemented.
- Preserves the open Workspace-versus-Room decision in `docs/context/DECISIONS.md`; no terminology decision or alias is introduced by this implementation.

- [ ] **Step 1: Write documentation verification checks**

  Add a checklist to the service README and context docs for `POST`, both `GET` endpoints, `X-User-Id` being local/test-only, production requiring trusted authentication, port `8082`, database `workspace_db`, the exact invitation statuses/structural constraints, and the explicitly deferred capabilities. Keep the Workspace-versus-Room terminology question aligned with `docs/context/DECISIONS.md`. No product-code test is required for prose-only files.

- [ ] **Step 2: Run documentation consistency checks**

  Run: `rg -n "workspace-service|workspace_db|8082|X-User-Id|trusted authentication|JWT|PENDING|ACCEPTED|DECLINED|EXPIRED|Workspace|Room|realtime|broker" AGENTS.md docs/context services/workspace-service/AGENTS.md services/workspace-service/README.md services/workspace-service/docs/context`

  Expected: the new service is listed as implemented only for the delivered slice; the header adapter is explicitly local/test-only; trusted production authentication is a prerequisite; JWT production auth, invitation workflows, documents, realtime, and broker remain labeled deferred/planned; Workspace-versus-Room remains open.

- [ ] **Step 3: Run the complete test suite**

  Run: `.\mvnw.cmd test`

  Expected: PASS with all unit, web, persistence, and context tests green. The default suite uses H2 only for isolated tests and does not select `WorkspacePostgresMigrationSmokeIT`. Separately run the PostgreSQL smoke test with the existing Compose database and a temporary isolated database:

  ```powershell
  $composeArgs = @('-f', 'docker-compose.yml')
  $containerId = $null
  $startedHere = $false
  $databaseName = $null
  $databaseCreated = $false
  $smokeExit = 1
  $hadPreviousTestUrl = $null -ne [Environment]::GetEnvironmentVariable('WORKSPACE_POSTGRES_TEST_URL', 'Process')
  $previousTestUrl = [Environment]::GetEnvironmentVariable('WORKSPACE_POSTGRES_TEST_URL', 'Process')
  try {
      $composePsOutput = @(& docker compose @composeArgs ps -q postgres 2>$null)
      if ($LASTEXITCODE -ne 0) { throw 'Could not inspect the existing PostgreSQL Compose service' }
      if ($composePsOutput.Count -gt 0 -and -not [string]::IsNullOrWhiteSpace([string]$composePsOutput[0])) {
          $containerId = ([string]$composePsOutput[0]).Trim()
      }
      if ([string]::IsNullOrWhiteSpace($containerId)) {
          $startedHere = $true
          & docker compose @composeArgs up -d postgres
          if ($LASTEXITCODE -ne 0) { throw 'Could not start the existing PostgreSQL Compose service' }
          $composePsOutput = @(& docker compose @composeArgs ps -q postgres 2>$null)
          if ($LASTEXITCODE -ne 0) { throw 'Could not inspect PostgreSQL after startup' }
          if ($composePsOutput.Count -gt 0 -and -not [string]::IsNullOrWhiteSpace([string]$composePsOutput[0])) {
              $containerId = ([string]$composePsOutput[0]).Trim()
          }
          if ([string]::IsNullOrWhiteSpace($containerId)) { throw 'PostgreSQL container id was not returned after startup' }
      }
      $ready = $false
      for ($attempt = 0; $attempt -lt 30; $attempt++) {
          & docker exec $containerId pg_isready -U workspace_service -d workspace_db *> $null
          if ($LASTEXITCODE -eq 0) { $ready = $true; break }
          Start-Sleep -Seconds 1
      }
      if (-not $ready) { throw 'PostgreSQL did not become ready' }
      $databaseName = 'workspace_smoke_' + ([guid]::NewGuid().ToString('N').Substring(0, 12))
      & docker exec $containerId psql -U workspace_service -d workspace_db -v ON_ERROR_STOP=1 -c "CREATE DATABASE $databaseName"
      if ($LASTEXITCODE -ne 0) { throw 'Could not create the isolated smoke database' }
      $databaseCreated = $true
      $env:WORKSPACE_POSTGRES_TEST_URL = "jdbc:postgresql://localhost:5433/$databaseName"
      .\mvnw.cmd -Dtest=WorkspacePostgresMigrationSmokeIT test
      $smokeExit = $LASTEXITCODE
  }
  finally {
      if ($databaseCreated -and -not [string]::IsNullOrWhiteSpace($containerId)) {
          & docker exec $containerId psql -U workspace_service -d workspace_db -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS $databaseName"
          $dropExit = $LASTEXITCODE
          if ($dropExit -ne 0) {
              Write-Error "Failed to drop temporary PostgreSQL database '$databaseName' (exit code $dropExit)."
              if ($smokeExit -eq 0) { $smokeExit = $dropExit }
          }
      }
      if ($hadPreviousTestUrl) {
          $env:WORKSPACE_POSTGRES_TEST_URL = $previousTestUrl
      } else {
          Remove-Item Env:WORKSPACE_POSTGRES_TEST_URL -ErrorAction SilentlyContinue
      }
      if ($startedHere) { & docker compose @composeArgs stop postgres | Out-Null }
  }
  if ($smokeExit -ne 0) { exit $smokeExit }
  ```

  Expected: Flyway applies `V1__create_workspace_tables.sql` and Hibernate schema validation passes against PostgreSQL in the temporary database. The command never calls `docker compose down`, never removes the named `workspace_postgres_data` volume, drops only the generated temporary database, verifies that the drop succeeded and reports cleanup failure explicitly (also failing the smoke step if the test itself passed), restores the previous `WORKSPACE_POSTGRES_TEST_URL` value, and stops PostgreSQL only when this step started it. No foreign database, container, or volume is targeted.

- [ ] **Step 4: Inspect the final diff**

  Run:

  ```powershell
  $baselineLine = Get-Content '.superpowers/sdd/2026-10-08-workspace-service-layered-structure/progress.md' | Select-String '^BASE_COMMIT: ' | Select-Object -Last 1
  if (-not $baselineLine) { throw "Exact BASE_COMMIT is missing from the execution ledger" }
  $baseCommit = $baselineLine.Line.Substring(13).Trim()
  if ($baseCommit -notmatch '^[0-9a-f]{40}$') { throw "Exact BASE_COMMIT is missing from the execution ledger" }
  git diff --check "$baseCommit..HEAD"
  git diff --cached --check
  git diff --check
  git status --short --untracked-files=all
  git diff --stat "$baseCommit..HEAD"
  git diff --cached --name-status
  git diff --name-status
  git ls-files --others --exclude-standard
  ```

  Expected: no whitespace errors in committed, staged, or unstaged changes; the committed diff from the exact ledger baseline, staged diff, unstaged diff, and untracked file list are all inspected. Only intended workspace-service implementation/context changes are accepted; pre-existing `.idea/` remains untouched and any unrelated changes are reported before commit.

- [ ] **Step 5: Commit documentation and verification updates**

  ```bash
  git add -- AGENTS.md docs/context services/workspace-service/AGENTS.md services/workspace-service/README.md
  git add -f -- services/workspace-service/docs/context
  git commit -m "docs: document workspace service boundaries"
  ```
