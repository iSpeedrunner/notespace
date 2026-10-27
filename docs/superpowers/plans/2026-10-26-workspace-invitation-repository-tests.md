# Workspace Invitation Repository Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add H2 repository tests for saving a `PENDING` workspace invitation and retrieving it by ID.

**Architecture:** Keep the existing `@SpringBootTest` repository-test approach. Save the parent `Workspace` first, then save the invitation through the real `WorkspaceInvitationRepository` and verify persisted state.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring Data JPA, JUnit Jupiter, H2, Lombok.

**Spec:** `docs/superpowers/specs/2026-10-26-workspace-invitation-repository-tests-design.md`

## Global Constraints

- Use the existing `services/workspace-service/src/test/resources/application-test.yaml` H2 profile.
- Do not change repository interfaces, migrations, or production service behavior.
- Persist a `Workspace` before assigning it to `WorkspaceInvitation`.
- Set `status` to `WorkspaceInvitationStatus.PENDING` in the save test.

## Review Focus

- A transient workspace must not be assigned directly to an invitation; the parent must be saved first.
- `findById()` must return the invitation with the same ID, not merely a non-null repository result.
- The persisted enum value must remain `PENDING`.
- Invitation timestamps and IDs should be populated consistently with the production schema.

### Task 1: Workspace invitation entity accessors and repository tests

**Files:**
- Modify: `services/workspace-service/src/main/java/com/notespace/workspace_service/entity/WorkspaceInvitation.java`
- Modify: `services/workspace-service/src/test/java/com/notespace/workspace_service/repository/WorkspaceInvitationRepositoryTest.java`

**Interfaces:**
- Consumes: `WorkspaceRepository`, `WorkspaceInvitationRepository`, `WorkspaceInvitationStatus.PENDING`.
- Produces: `WorkspaceInvitation.builder()` for test fixture construction and two executable repository tests.

- [x] **Step 1: Write the failing tests**

  Add `shouldSavePendingWorkspaceInvitation()` that saves a persisted workspace and invitation, then asserts the invitation ID and `PENDING` status. Add `shouldFindWorkspaceInvitationById()` that saves an invitation, calls `findById(saved.getId())`, and asserts the result is present with the same ID and status.

- [x] **Step 2: Run the focused tests to verify the expected failure**

  Run `mvn -Dmaven.repo.local=<workspace-service>/.m2/repository -Dtest=WorkspaceInvitationRepositoryTest test` from `services/workspace-service/`. The first run may fail to compile because `WorkspaceInvitation` currently has no builder/accessors.

- [x] **Step 3: Add Lombok accessors and builder**

  Add `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, and `@Builder` to `WorkspaceInvitation`. Keep the existing JPA mapping and repository unchanged.

- [x] **Step 4: Run the focused tests to verify they pass**

  Re-run the focused Maven command and confirm both tests pass.

- [x] **Step 5: Run the workspace-service test suite**

  Run `mvn -Dmaven.repo.local=<workspace-service>/.m2/repository test` and report any unrelated failures separately.
