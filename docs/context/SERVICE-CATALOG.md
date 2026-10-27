# Service catalog

## Current services

| Service | Status | Responsibility | Database | Port | Context |
| --- | --- | --- | --- | --- | --- |
| `user-service` | Implemented; Phase 1 in progress | Account registration, authentication, access/refresh token handling, current-user lookup | PostgreSQL `user_db` | HTTP 8081; local PostgreSQL 5432 | [`services/user-service/docs/context/INDEX.md`](../../services/user-service/docs/context/INDEX.md) |
| `workspace-service` | Create/read MVP implemented; broader Phase 2 in progress | Workspace creation with OWNER, membership-scoped list/get, invitation persistence only | PostgreSQL `workspace_db` | HTTP 8082; local PostgreSQL 5433 | [Workspace context](../../services/workspace-service/docs/context/INDEX.md) |

Port and database configuration: `services/user-service/src/main/resources/application.yaml`; local DB container: `services/user-service/docker-compose.yml`.

## Planned services

These do not currently exist in the repository.

| Service | Planned responsibility |
| --- | --- |
| `realtime-service` | WebSocket collaboration sessions, Yjs synchronization, online users and presence |
| `notification-service` | Email, push/in-app notification delivery and integration-event processing |

See [[ROADMAP]] and [[NEW-SERVICE]].
