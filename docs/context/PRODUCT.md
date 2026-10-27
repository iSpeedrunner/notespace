# Product context

NoteSpace is a collaborative workspace and note-taking product: users belong to shared workspaces/rooms, work on documents, and may eventually edit together in realtime. Notion-style workspaces and Google Docs-style collaboration describe product intent only; they are not technical requirements.

## Domain concepts and status

| Concept | Meaning | Repository status |
| --- | --- | --- |
| User | Account identity and credentials | Implemented in `user-service` (`User`, `Role`, refresh token); see its [[DOMAIN]]. |
| Workspace / Room | Shared container for members and documents | Workspace create/read MVP implemented; document operations planned. |
| Membership | Association of a user with a workspace | Implemented persistence and membership-scoped reads; create adds OWNER atomically. |
| Role / Permission | Membership capabilities and access rules | Workspace OWNER/MEMBER persistence implemented; full permissions planned. Account USER/ADMIN roles in user-service are separate. |
| Document | Content and lifecycle within a workspace | Planned. |
| Collaborative Session | Realtime editing session for a document | Planned. |
| Presence | Current participants / activity in a collaborative session | Planned. |

Conceptually:

```text
User ── Membership ── Workspace / Room ── Documents ── Collaborative Session
                                                           └── Presence
```

User/account and workspace create/read capabilities exist in source. Invitations have persistence only. See [[ROADMAP]] for status by phase.

## Terminology question

The product direction uses both “Workspace” and “Room”; no code currently establishes which is canonical. Keep terminology as-is in new docs/code until a product decision resolves this. Do not rename existing identifiers as part of documentation work.
