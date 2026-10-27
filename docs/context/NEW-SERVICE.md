# Adding a service

Use this checklist only when a task requires a new service; do not create empty scaffolding for planned services.

## Before creating a service

1. Define its business capability and boundaries.
2. Confirm the capability does not belong in an existing service.
3. Name the data it owns and the persistence model it controls.
4. Define API/event contracts and their ownership.
5. Identify dependencies and failure behavior.
6. Decide whether each interaction needs synchronous or asynchronous communication.
7. Avoid shared database access; each service owns its data.
8. Select technology that serves a concrete requirement and fits repository operations.

## Structure

Follow established repository conventions and only add files the service actually needs. Conceptual shape:

```text
services/<service-name>/
├── AGENTS.md
├── README.md
├── build file
├── Dockerfile       # when application containerization is established/required
├── src/
└── docs/context/    # when service is sufficiently complex
```

There is no current repository-wide application Dockerfile convention: the existing user-service Dockerfile is a placeholder. Do not copy it as a working template.

## After creation

Add accurate system and local guidance; update [[SERVICE-CATALOG]], [[ARCHITECTURE]], [[DATA-OWNERSHIP]], and [[COMMUNICATION]], plus root `AGENTS.md` when routing/rules change. A sufficiently complex service should eventually have its own `AGENTS.md` and useful `docs/context/`; do not create empty docs for nonexistent services.
