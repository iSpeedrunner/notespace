# Data ownership

## Current implementation

| Data | Owner | Persistence | Other services access through |
| --- | --- | --- | --- |
| User identity, username, email, password hash, account role, timestamps | `user-service` | PostgreSQL `user_db`, tables `users`, `refresh_token` | No cross-service integration exists; workspace-service holds opaque user UUIDs. |
| Refresh-token hashes, expiry, revocation state | `user-service` | PostgreSQL `user_db`, table `refresh_token` | Raw tokens are returned by auth endpoints; hashes remain internal persistence data. |
| Workspaces, memberships, invitation persistence | `workspace-service` | PostgreSQL `workspace_db`: `workspace`, `workspace_member`, `workspace_invitation` | Three create/read REST endpoints with caller identity and membership authorization; no cross-service consumer yet. |

These ownership facts are based on `User`, `RefreshToken`, migrations V1–V3, and user-service configuration. Schema detail: [DATABASE](../../services/user-service/docs/context/DATABASE.md).

## Rule

A microservice must never read or modify another microservice's database directly. Obtain cross-service data through explicit REST/HTTP contracts or, if later justified, integration events. No cross-service database access is present in this repository.

## Planned ownership

| Data | Planned owner | Access from other services |
| --- | --- | --- |
| Additional workspace/member/invitation lifecycle, full permissions, document metadata/lifecycle | Existing `workspace-service`, future capabilities | Explicit API/event contracts |
| Realtime session and presence state | Planned `realtime-service`; Redis is only a possible backing store | Realtime protocol/service contracts |
| Notification delivery state | Planned `notification-service` | Explicit integration events and APIs |

Planned rows describe future capabilities. Workspace/member/invitation tables already exist; invitations have no API/lifecycle. No FK references user_db.
