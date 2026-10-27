# Workspace service context

IMPLEMENTED: workspace creation with OWNER membership in one transaction; list/get through membership-filtered JPQL; three REST endpoints at `/api/v1/workspaces`; DTO validation; consistent JSON errors; profile-restricted development identity; persistence for workspaces, memberships and invitations.

See [README](../../README.md) for API, profiles, test commands and limitations. Layers: controller → WorkspaceApplicationService → repositories → entities. HTTP identity is adapted through CurrentUserProvider; application methods accept trusted user UUIDs without servlet/JWT dependencies. Hibernate validates Flyway's PostgreSQL schema. No direct access to user-service or its database.

Executed checks and exact results: [VERIFICATION](VERIFICATION.md).

Executed checks and exact results: [VERIFICATION](VERIFICATION.md).

Invariants: canonical workspace name 1–100 chars; create atomically adds OWNER; unique membership per workspace/user; OWNER and MEMBER only; users can belong to many workspaces. All user references are UUIDs. Invitation statuses are PENDING, ACCEPTED, DECLINED, EXPIRED; persistence only, with no lifecycle behavior.

Security: default/prod/production reject requests with 401 until a trusted identity provider is supplied. Explicit local/test accepts one canonical UUID header; prod/production takes precedence over development profiles. No fake-user fallback, Basic login or JWT parser. Foreign and missing workspace both produce WORKSPACE_NOT_FOUND (404).

IN PROGRESS: broader workspace capability and production readiness. PLANNED: production authentication integration, workspace mutations, member/invitation lifecycle, documents and realtime collaboration.

Reconciled 2026-10-27: prior root context said workspace-service did not exist; actual code already had models, migration, repositories and a transactional create implementation. Existing V1 was retained, including required invitation expiry.
