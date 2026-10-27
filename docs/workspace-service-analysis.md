# NoteSpace `workspace-service`: repository analysis and implementation plan

Дата аналізу: 2026-10-07

Цей документ підготовлено read-only аналізом репозиторію. Код, існуючі файли, коміти та незакомічені зміни під час аналізу не змінювалися.

## 1. Current repository state

У репозиторії source-verified реалізований `services/user-service`:

- Java 21, Spring Boot 4.1.1;
- Spring MVC, Spring Security, Spring Data JPA;
- PostgreSQL та Flyway;
- JWT access tokens;
- refresh-token rotation/revocation.

Джерела:

- [root AGENTS.md](../AGENTS.md)
- [system context](context/INDEX.md)
- [user-service context](../services/user-service/docs/context/INDEX.md)

### Важлива невідповідність

Root documentation стверджує, що `workspace-service` ще не створений. Фактично committed scaffold уже існує:

- `services/workspace-service/pom.xml`;
- `docker-compose.yml`;
- `WorkspaceServiceApplication.java`;
- `application.yaml`;
- context-load test.

У робочому дереві також є untracked:

- `Workspace.java`;
- `WorkspaceMember.java`;
- `WorkspaceRole.java`.

Ці класи є порожніми та не мають JPA-анотацій. Controllers, services, repositories, DTO, migrations, security, exceptions і бізнес-тести відсутні.

Поточний Git state під час аналізу:

```text
Branch: feat/workspace-service-setup
HEAD: 8370dda

Untracked:
.idea/
services/workspace-service/src/main/java/com/notespace/workspace_service/entity/
```

Поточний scaffold:

```text
HTTP port: 8082
Database: workspace_db
Database host port: 5433
Database user: workspace_service
Hibernate ddl-auto: validate
Flyway: enabled
```

Це bootstrap, а не реалізований бізнес-сервіс.

## 2. Proposed workspace-service responsibility

### Відповідальність

- workspace lifecycle;
- workspace metadata;
- membership;
- workspace roles;
- workspace authorization rules;
- invitations.

### Не відповідатиме за

- registration, password hashing, login;
- refresh tokens або JWT issuing;
- user profile persistence;
- account roles `USER` / `ADMIN`;
- realtime editing, WebSocket, Yjs, presence;
- notifications та email delivery;
- `user_db`.

`user-service` залишається owner для identity та authentication.

## 3. Workspace / Room terminology

Документація використовує обидва терміни. [DECISIONS.md](context/DECISIONS.md) фіксує це як open decision; source code canonical naming не встановлює.

### Рекомендація

Використовувати `Workspace` як canonical domain term: він збігається з назвою сервісу та вже підготовленого scaffold. `Room` можна залишити product/UI synonym. Не створювати окремі `Workspace` і `Room` сутності.

## 4. Domain model

### Workspace

| Field | Recommendation |
|---|---|
| `id` | UUID primary key |
| `name` | required, trimmed, non-empty, bounded length |
| `ownerUserId` | UUID, без FK до `user_db` |
| `createdAt` | UTC instant |
| `updatedAt` | UTC instant |

На MVP не потрібні slug, description, avatar, billing plan, settings JSON, audit log або soft-delete.

### WorkspaceMember

| Field | Recommendation |
|---|---|
| `id` | UUID primary key |
| `workspaceId` | FK до `workspace` в `workspace_db` |
| `userId` | UUID, без FK до `user_db` |
| `role` | workspace-specific enum |
| `joinedAt` | UTC instant |

Обов’язково: `UNIQUE (workspace_id, user_id)`.

### WorkspaceInvitation

Invitation потрібна в MVP, оскільки вона входить до Phase 2.

Рекомендовані поля:

| Field | Recommendation |
|---|---|
| `id` | UUID |
| `workspaceId` | internal FK |
| `inviteeUserId` | UUID без FK до `user_db` |
| `invitedByUserId` | UUID без FK до `user_db` |
| `status` | `PENDING`, `ACCEPTED`, `DECLINED`, `EXPIRED` |
| `expiresAt` | UTC instant |
| `createdAt` | UTC instant |
| `respondedAt` | nullable UTC instant |

Для MVP достатньо запрошення конкретного user за UUID. Raw token і email delivery не потрібні. Для одного workspace/user має бути не більше одного `PENDING` invitation.

## 5. Roles and permissions

Account roles із `user-service` (`USER`, `ADMIN`) не є workspace roles.

Рекомендований набір:

```text
OWNER
ADMIN
MEMBER
```

- **OWNER:** повний доступ, включно з delete workspace і role changes; не може leave без ownership transfer.
- **ADMIN:** перегляд, додавання members, видалення `MEMBER`, invitations; не змінює owner і не видаляє workspace.
- **MEMBER:** перегляд і leave; не адмініструє membership, roles або workspace.

На MVP рекомендується один owner на workspace.

## 6. Domain invariants

### Обов’язкові для MVP

1. Workspace створює authenticated user.
2. Creator автоматично стає member з роллю `OWNER`.
3. Workspace не існує без owner.
4. В одному workspace є рівно один owner.
5. `(workspaceId, userId)` унікальна membership.
6. Duplicate membership повертає `409 Conflict`.
7. Non-member не має доступу.
8. Видалити workspace може лише `OWNER`.
9. `OWNER` не може leave без ownership transfer.
10. `ADMIN` не може змінити owner.
11. `MEMBER` не може змінювати roles.
12. Invitation приймає лише invitee.
13. Expired invitation не приймається.
14. Pending invitations не дублюються.
15. Видалення workspace каскадно видаляє members та invitations.
16. Немає FK до `user_db`.

### Пізніше

Ownership transfer, кілька owners, custom roles, permission overrides, revoke/resend invitations, audit trail, soft deletion, account deletion synchronization, email invitations і bulk membership operations.

## 7. Database model

```text
workspace
---------
id UUID PK
name
owner_user_id UUID
created_at
updated_at
        |
        | 1:N
        v
workspace_member
----------------
id UUID PK
workspace_id UUID FK -> workspace.id
user_id UUID
role
joined_at

UNIQUE(workspace_id, user_id)

workspace_invitation
--------------------
id UUID PK
workspace_id UUID FK -> workspace.id
invitee_user_id UUID
invited_by_user_id UUID
status
expires_at
created_at
responded_at
```

User identifiers не мають foreign keys у `user_db`. Consistency між `workspace.owner_user_id` і `workspace_member(role = OWNER)` enforce-иться application transaction.

## 8. REST API

Рекомендований prefix: `/api/v1`.

### Workspace

| Method | Path | Auth | Permission | Request | Response | Errors |
|---|---|---|---|---|---|---|
| POST | `/api/v1/workspaces` | Authenticated | Any authenticated user | `{name}` | `201 WorkspaceResponse` | 400, 401 |
| GET | `/api/v1/workspaces` | Authenticated | Any authenticated user | — | `200 WorkspaceSummary[]` | 401 |
| GET | `/api/v1/workspaces/{id}` | Authenticated | Member | — | `200 WorkspaceResponse` | 401, 403, 404 |
| PATCH | `/api/v1/workspaces/{id}` | Authenticated | OWNER or ADMIN | `{name}` | `200 WorkspaceResponse` | 400, 401, 403, 404 |
| DELETE | `/api/v1/workspaces/{id}` | Authenticated | OWNER | — | `204` | 401, 403, 404 |

### Membership

| Method | Path | Auth | Permission | Request | Response | Errors |
|---|---|---|---|---|---|---|
| GET | `/api/v1/workspaces/{id}/members` | Authenticated | Member | — | `200 MemberResponse[]` | 401, 403, 404 |
| POST | `/api/v1/workspaces/{id}/members` | Authenticated | OWNER or ADMIN | `{userId, role?}` | `201 MemberResponse` | 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/workspaces/{id}/members/{userId}` | Authenticated | OWNER or ADMIN | — | `204` | 401, 403, 404, 409 |
| POST | `/api/v1/workspaces/{id}/leave` | Authenticated | Current member | — | `204` | 401, 403, 404, 409 |
| PATCH | `/api/v1/workspaces/{id}/members/{userId}/role` | Authenticated | OWNER | `{role}` | `200 MemberResponse` | 400, 401, 403, 404, 409 |

### Invitations

| Method | Path | Auth | Permission | Request | Response | Errors |
|---|---|---|---|---|---|---|
| POST | `/api/v1/workspaces/{id}/invitations` | Authenticated | OWNER or ADMIN | `{userId}` | `201 InvitationResponse` | 400, 401, 403, 404, 409 |
| GET | `/api/v1/workspaces/{id}/invitations` | Authenticated | OWNER or ADMIN | — | `200 InvitationResponse[]` | 401, 403, 404 |
| POST | `/api/v1/invitations/{id}/accept` | Authenticated | Invitee only | — | `200 MemberResponse` | 401, 403, 404, 409 |
| POST | `/api/v1/invitations/{id}/decline` | Authenticated | Invitee only | — | `204` | 401, 403, 404, 409 |

Помилки: validation `400`, unauthenticated `401`, forbidden `403`, missing resource `404`, duplicate/conflict `409`.

## 9. Authentication strategy

Поточний JWT у `user-service` має HMAC signature, `sub=email`, `iat) і expiration приблизно 15 хвилин. Claim `userId`, issuer і audience відсутні.

### Рекомендований варіант

Додати до access JWT:

```text
sub: email
userId: UUID
iat
exp
```

`workspace-service` локально перевіряє token і бере current user UUID із `userId`. HTTP lookup на кожен request не потрібен. `sub=email` можна тимчасово зберегти для backward compatibility.

Shared HMAC прийнятний для MVP, але пізніше варто перейти на asymmetric signing: user-service володіє private key, інші сервіси — public key. Також бажано додати issuer/audience і явно налаштувати stateless sessions.

## 10. Interaction with user-service

HTTP contract потрібен лише для target-user validation: додавання member, створення invitation і майбутньої перевірки account status.

Поточний user-service має лише `/api/v1/users/me`. Рекомендований окремий контракт:

```text
GET /api/v1/users/{userId}

200 { id, username, email }
404 user not found
```

Workspace-service зберігає лише `userId`. Якщо user-service недоступний, membership/invitation не створюється; повертається `503` або стабільна dependency error. Retries, broker, cache і circuit breaker не потрібні для MVP.

## 11. Package / directory structure

Поточний scaffold використовує `com.notespace.workspace_service), тоді як user-service використовує `com.notespace.userservice). Рекомендований canonical package — `com.notespace.workspaceservice`; це потрібно вирішити до стабілізації API.

```text
services/workspace-service/
├── AGENTS.md
├── README.md
├── pom.xml
├── docker-compose.yml
├── src/main/java/com/notespace/workspaceservice/
│   ├── WorkspaceServiceApplication.java
│   ├── config/
│   ├── security/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   ├── exception/
│   └── client/
├── src/main/resources/db/migration/
├── src/test/java/com/notespace/workspaceservice/
└── docs/context/
```

## 12. Dependencies

### Required

- `spring-boot-starter-webmvc`;
- `spring-boot-starter-validation`;
- `spring-boot-starter-data-jpa`;
- `spring-boot-starter-flyway`;
- `flyway-database-postgresql`;
- `postgresql);
- `spring-boot-starter-security`;
- `jjwt-api`, `jjwt-impl), `jjwt-jackson`;
- `spring-boot-starter-actuator`.

### Tests

Spring MVC test, Spring Security test, Spring Data JPA test, Flyway test, JUnit 5/Mockito. Testcontainers PostgreSQL рекомендований для integration tests.

### Не додавати

Kafka, RabbitMQ, Redis, WebSocket, Yjs, Gateway, notification client, Kubernetes, event bus, tracing і circuit breaker.

## 13. Flyway migration plan

### V1 — workspaces

Таблиця `workspace`: UUID PK, name, owner_user_id, created_at, updated_at. FK до `user_db` не створювати.

### V2 — members

Таблиця `workspace_member) з FK до workspace, role, joined_at та unique `(workspace_id, user_id)). Додати indexes для workspace_id і user_id.

### V3 — invitations

Таблиця `workspace_invitation` з FK до workspace, invitee/inviter IDs, status, expires_at, created_at, responded_at. Додати indexes для workspace_id, invitee_user_id і `(status, expires_at)`. Для pending invitations — partial unique index або еквівалентне транзакційне обмеження.

## 14. Testing strategy

### Unit tests

- create workspace і automatic OWNER;
- update/delete permissions;
- duplicate membership;
- add/remove/leave;
- owner protection;
- role changes;
- invitation lifecycle;
- expired invitation;
- wrong invitee;
- invalid workspace/member;
- authorization matrix.

### Controller/security tests

HTTP status, validation, DTO, error body, current principal, valid/invalid/expired JWT, 401 і 403.

### Integration tests

Flyway migrations, JPA mappings, PostgreSQL constraints, cascade delete, transaction create workspace + owner membership, pending invitation uniqueness і rollback behavior. Найкращий варіант — Testcontainers PostgreSQL.

## 15. Implementation order

### STEP 1 — authentication foundation

**Goal:** Узгодити JWT contract і current-user boundary.

**Files/components:** user-service JWT generation, workspace-service security/config, tests.

**What to implement:** додати `userId` claim, JWT verification, `CurrentUser`, stateless security.

**Tests:** valid/invalid/expired JWT і token без `userId`.

**Definition of Done:** workspace-service отримує UUID без user DB і без HTTP call на кожен request.

**Branch:** `feat/workspace-auth-contract`.

### STEP 2 — database bootstrap

**Goal:** Налаштувати database/Flyway/health configuration та V1 migration.

**Tests:** context startup, Flyway, schema validation.

**Definition of Done:** чиста `workspace_db) успішно стартує.

**Branch:** `feat/workspace-bootstrap`.

### STEP 3 — Workspace domain

**Goal:** Entity, repository, DTO і V1 migration.

**Tests:** mapping, validation, persistence, missing workspace.

**Branch:** `feat/workspace-domain`.

### STEP 4 — Membership domain

**Goal:** Entity, roles, repository і V2 migration.

**Tests:** duplicate membership, role persistence, owner membership, cascade.

**Branch:** `feat/workspace-membership`.

### STEP 5 — Workspace CRUD

**Goal:** Create/list/get/patch/delete, включно з atomic owner membership creation.

**Tests:** CRUD, creator owner, access control, invalid workspace.

**Branch:** `feat/workspace-crud`.

### STEP 6 — Authorization

**Goal:** Явний OWNER/ADMIN/MEMBER permission matrix.

**Tests:** role matrix, non-member, 401/403.

**Branch:** `feat/workspace-authorization`.

### STEP 7 — Membership API

**Goal:** List/add/remove/leave/change role.

**Tests:** duplicate, owner protection, leave behavior, role restrictions.

**Branch:** `feat/workspace-membership-api`.

### STEP 8 — Invitations

**Goal:** Persist and expose invitation lifecycle.

**Tests:** create/accept/decline/expire/duplicate/wrong invitee.

**Branch:** `feat/workspace-invitations`.

### STEP 9 — user-service contract

**Goal:** Explicit target-user lookup через HTTP.

**Tests:** existing user, missing user, user-service unavailable, rollback.

**Branch:** `feat/workspace-user-contract`.

### STEP 10 — Full verification

**Goal:** Integration, migration and security coverage проти clean PostgreSQL.

**Branch:** `test/workspace-service-coverage`.

### STEP 11 — Documentation

**Goal:** Оновити system/service context після source implementation.

**Branch:** `docs/workspace-service-context`.

## 16. Risks

### Architecture

- **Workspace/Room naming:** може породити дубльовані сутності. Mitigation: зафіксувати Workspace canonical.
- **Scope expansion:** documents/realtime можуть передчасно потрапити в сервіс. Mitigation: обмежити Phase 2.

### Security

- **Shared HMAC:** сервіс із secret може mint-ити tokens. Mitigation: MVP only, пізніше asymmetric signing.
- **JWT lacks userId:** немає стабільної identity для membership. Mitigation: додати claim.
- **No stateless policy:** налаштувати stateless.
- **No issuer/audience:** додати claims перед масштабуванням.

### Database

- **No FK до user_db:** можливі orphan identifiers. Mitigation: explicit user lookup.
- **Owner consistency:** workspace і membership можуть розійтися. Mitigation: atomic transaction.
- **Timestamp semantics:** використовувати `Instant), не `LocalDateTime`.

### API/coupling

- Role changes можуть спричинити privilege escalation — ownership transfer відкласти.
- Error formats можуть розійтися — стабілізувати error contract.
- Не викликати user-service на кожен request.
- Не дублювати username/email у workspace database.

## 17. Documentation impact

Після реалізації оновити:

- `docs/context/SERVICE-CATALOG.md`;
- `docs/context/ARCHITECTURE.md`;
- `docs/context/DATA-OWNERSHIP.md`;
- `docs/context/COMMUNICATION.md`;
- `docs/context/ROADMAP.md`;
- `docs/context/PRODUCT.md) після naming decision;
- `docs/context/DECISIONS.md);
- root `AGENTS.md).

Для workspace-service потрібні:

```text
AGENTS.md
README.md
docs/context/INDEX.md
docs/context/SERVICE.md
docs/context/API.md
docs/context/DOMAIN.md
docs/context/DATABASE.md
docs/context/SECURITY.md
docs/context/FLOWS.md
docs/context/TESTING.md
```

Документація повинна описувати лише source-verified behavior.

## 18. Open architecture decisions

1. `Workspace` чи `Room` — canonical term.
2. Додавати `userId` claim до JWT.
3. Чи залишати `sub=email` для compatibility.
4. Чи потрібні issuer/audience в MVP.
5. Один owner чи кілька.
6. Ownership transfer.
7. Максимальна довжина workspace name.
8. Target-user lookup API.
9. Invitation by UUID чи email.
10. Exact error contract.
11. Package `workspace_service) чи `workspaceservice).
12. Testcontainers timing.
13. Document ownership у Phase 3.
14. User deletion/deactivation behavior.

## 19. Recommended first coding task

Реалізувати лише authentication foundation для workspace-service:

1. Додати до access JWT у user-service стабільний claim `userId`, зберігши `sub=email`.
2. У workspace-service реалізувати JWT validation на основі спільного secret.
3. Додати stateless Spring Security configuration.
4. Додати current-user abstraction, яка повертає UUID із `userId` claim.
5. Додати unit/security tests для valid JWT, invalid JWT, expired JWT і JWT без `userId`.
6. Не реалізовувати Workspace, membership, invitations, controllers або database tables.
7. Не додавати Kafka, Redis, Gateway, WebSocket чи HTTP lookup до user-service.
8. Оновити security documentation, оскільки змінюється JWT contract.

Це найменший корисний slice: він вирішує критичну межу між authentication і workspace authorization без передчасної domain/API complexity.
