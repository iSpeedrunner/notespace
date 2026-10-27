# Product direction and implementation status

Reconciled with source on 2026-10-27. IMPLEMENTED means source exists; production readiness and verification are stated separately.

## Phase 1 - AUTH

**Status: IN PROGRESS**. Owner: user-service. Registration, login, JWT access tokens, hashed/rotated/revocable refresh tokens, current-user lookup and logout exist. Request validation, Spring Security and selected global error handlers exist. Unit tests are present; comprehensive endpoint/database integration coverage remains planned. This task did not change user-service.

## Phase 2 - WORKSPACES / ROOMS

**Status: IN PROGRESS**. Owner: existing workspace-service. Create/read MVP IMPLEMENTED; broader workspace lifecycle and production identity integration remain PLANNED.

| Capability | Status |
| --- | --- |
| Workspace, WorkspaceMember, WorkspaceInvitation persistence | IMPLEMENTED |
| OWNER/MEMBER roles, unique membership, workspace foreign keys | IMPLEMENTED |
| Atomic create with OWNER and normalized name validation | IMPLEMENTED |
| POST /api/v1/workspaces | IMPLEMENTED |
| GET /api/v1/workspaces and GET /api/v1/workspaces/{workspaceId} | IMPLEMENTED; membership-scoped queries, same 404 for foreign/missing |
| DTO validation and JSON errors | IMPLEMENTED |
| Temporary X-User-Id identity | IMPLEMENTED; explicit local/test only; default/prod/production fails closed |
| H2 tests and isolated PostgreSQL/Flyway verification test | IMPLEMENTED; see service README and verification report |
| Trusted production JWT and cross-service identity contract | PLANNED |
| Workspace rename/delete and member join/leave/remove | PLANNED |
| Invitation API/lifecycle and full permissions | PLANNED |

## Phase 3 - DOCUMENTS

**Status: PLANNED**. Document lifecycle, metadata, permissions and editor do not exist. Likely owner: workspace-service, subject to design. Depends on workspace membership and explicit document contracts.

## Phase 4 - REALTIME

**Status: PLANNED**. Realtime-service, WebSocket, Yjs, collaboration, presence, typing indicators and optional Redis do not exist. Depends on document identity/access contracts.

## Phase 5 - EVENT-DRIVEN EXTENSIONS

**Status: PLANNED**. Brokers, integration events, notification-service and outbox if required remain future work. Add infrastructure only for a concrete requirement.

## Phase 6 - PRODUCTION

**Status: PLANNED / PARTIALLY PRESENT TOOLING**. PostgreSQL Compose files exist for both services. User-service Dockerfile is a placeholder. CI/CD, gateway, edge routing, HTTPS, monitoring and deployment remain planned. Workspace default/production endpoints remain unavailable without a trusted identity adapter; local/test identity is not production authentication.
