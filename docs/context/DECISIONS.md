# Architecture decisions

Compact record of choices established by the project direction or current source. This is not a record of undocumented historical discussions.

## ADR-001 — Service-owned data

**Status: Accepted**

**Decision:** Each microservice owns its persistence model; services must not access another service's database directly.

**Reason:** Established architecture rule for independent service boundaries.

**Consequences:** Cross-service data access requires an explicit API or future integration-event contract. See [[DATA-OWNERSHIP]].

## ADR-002 — Broker is not a baseline dependency

**Status: Accepted**

**Decision:** Introduce a message broker only for a concrete asynchronous integration requirement.

**Reason:** Event-driven extensions are roadmap direction; a broker is not currently mandatory.

**Consequences:** Current service remains broker-free. See [[COMMUNICATION]].

## Open — Workspace versus Room terminology

**Status: Open**

The product direction uses both terms and no source code resolves which is canonical. Keep this question open until a domain decision is made; see [[PRODUCT]].

## ADR-003 - Workspace development identity is profile-restricted

**Status: Accepted for MVP**

CurrentUserProvider separates workspace use cases from HTTP/JWT. A temporary X-User-Id adapter is enabled only for explicit local/test profiles. Prod/production overrides those profiles. Without a trusted provider, default/prod/production responds 401; no fixed user or unverified bearer token is trusted. Production JWT integration remains planned.
