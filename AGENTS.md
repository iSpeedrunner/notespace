# NoteSpace

Collaborative workspace and realtime document editing product. Repository contains `services/user-service` and `services/workspace-service`. Workspace create/read MVP exists; documents and realtime collaboration remain planned.

## Repository and current state

- `services/user-service/` — Java 21 / Spring Boot user and authentication service.
- `docs/context/` — system-level agent context; start at [[INDEX]].
- `services/workspace-service/` — Java 21 / Spring Boot workspace create/read MVP, memberships and invitation persistence. Production identity integration remains planned.
- Phase 1 AUTH is in progress. Treat only source-verified behavior as implemented; see [[ROADMAP]] and the user-service context.

## Architecture rules

- Each service owns a business capability and its data.
- Never read or modify another service's database directly.
- Keep cross-service contracts explicit; keep business logic independent from infrastructure where practical.
- Do not introduce infrastructure technologies without a current requirement or treat roadmap items as implemented.

## Service work

Before editing files inside `services/<service>/`, read `services/<service>/AGENTS.md` and the relevant service context. Check system context when a change crosses service boundaries.

When a code change alters architecture, service responsibility, public API, domain invariants, security behavior, database schema, important execution flows, data ownership, or integration contracts, update the corresponding `docs/context/` document. Do not update context for purely internal changes that do not alter documented behavior.
