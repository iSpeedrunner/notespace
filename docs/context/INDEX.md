# NoteSpace context index

System context for Codex and Obsidian. Current facts are verified against this repository; future direction is labeled in [[ROADMAP]] and related documents.

## Documents

[[PRODUCT]] · [[ROADMAP]] · [[ARCHITECTURE]] · [[SERVICE-CATALOG]] · [[DATA-OWNERSHIP]] · [[COMMUNICATION]] · [[CONVENTIONS]] · [[NEW-SERVICE]] · [[DECISIONS]]

## Context routing

| Task | Read |
| --- | --- |
| Understand product | [[PRODUCT]], [[ROADMAP]] |
| Change system architecture | [[ARCHITECTURE]], [[DECISIONS]] |
| Add microservice | [[NEW-SERVICE]], [[ARCHITECTURE]], [[DATA-OWNERSHIP]], [[COMMUNICATION]] |
| Modify existing service | [[SERVICE-CATALOG]] + service `AGENTS.md` and `docs/context/INDEX.md` |
| Add inter-service communication | [[COMMUNICATION]] |
| Change database ownership | [[DATA-OWNERSHIP]] |
| Introduce message broker | [[COMMUNICATION]], [[ARCHITECTURE]], [[DECISIONS]] |

Current services: [`user-service`](../../services/user-service/docs/context/INDEX.md) and [`workspace-service`](../../services/workspace-service/docs/context/INDEX.md). Workspace create/read MVP exists; production workspace identity integration is planned.
