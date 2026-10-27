# user-service

Purpose: account identity and authentication service. Source currently implements registration, login, JWT access tokens, refresh token rotation/revocation, current-user lookup, and an admin probe; see [service context](docs/context/INDEX.md) for verified details.

## Read first

- [docs/context/INDEX.md](docs/context/INDEX.md)
- Authentication: [SECURITY](docs/context/SECURITY.md), [FLOWS](docs/context/FLOWS.md)
- Database/domain: [DOMAIN](docs/context/DOMAIN.md), [DATABASE](docs/context/DATABASE.md)
- API changes: [API](docs/context/API.md)

## Rules

- Follow root architecture rules: each service owns its data; no cross-service database access.
- Do not treat roadmap items as implemented; verify current code first.
- When a code change alters architecture, service responsibility, public API, domain invariants, security behavior, database schema, important execution flows, data ownership, or integration contracts, update the corresponding `docs/context/` document. Skip documentation updates for internal changes that do not alter documented behavior.
