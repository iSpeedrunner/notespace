# Workspace MVP implementation plan

**Goal:** Complete the existing workspace-service using the user's detailed MVP specification.
**Architecture:** Keep package-by-layer, Java 21, existing Boot 4.1.1 dependencies, PostgreSQL/Flyway and H2 tests. User IDs remain opaque UUIDs. No user-service changes.
**Execution:** In place on feat/workspace-service-setup to retain existing uncommitted create implementation; snapshots are in service target/mvp-baseline. Never stage pre-existing service files or the pre-existing repository edit. No new worktree needed for this task.

- [x] Task 1: Test membership queries and generated invitation IDs; correct repository queries and explicit required JPA mappings. Preserve V1 unchanged. Run repository tests. 7 passed; commit 976128d.
- [x] Task 2: Test list/get visibility and missing/foreign equivalence; retain transactional create and its rollback test; implement read-only listForUser(userId), getForUser(userId, workspaceId). Run application tests. Intermediate suite: 16 passed. Final suite also tests rollback after SQL flush through real proxy.
- [x] Task 3: Test full HTTP flow and identity/profile failures; implement records, controller, JSON errors, profile-scoped identity adapter and stateless security. Missing trusted provider means 401, including default/production. Run suite. Final H2 suite: 40 passed. Framework error regression observed 500 instead of 405, then fixed and verified.
- [x] Task 4: Test Flyway, Hibernate validation and critical constraints against a new disposable PostgreSQL cluster/database. Run Java 21 suite, review diff, document exact results and remaining planned features. 40 H2 + 9 PostgreSQL tests passed; package and real HTTP smoke passed. See service docs/context/VERIFICATION.md.

Review focus: no membership bypass, malformed/duplicate header rejection, production plus test/local profile overlap fails closed, no owner from body, no persistence internals in errors.

Initial findings: system context incorrectly says only user-service exists. Existing create already normalizes and creates OWNER atomically. Existing uncommitted findByUserId prevents context startup. Invitation lacks UUID generation; member persistence tests omit required joinedAt. Docker daemon unavailable; PostgreSQL 18.1 binaries and Java 21 are installed locally.

Review: independent read-only reviewer found no Critical/Important findings. Fixed generic exception handler preserving framework 404/405/415; retained documented raw DTO 100-character limit from prior design, with canonical validation independently enforced in service. Application/REST remains uncommitted because its pre-existing create/provider/tests were untracked at task start; do not stage them implicitly. Documentation remains subject to existing repository Markdown ignore rules.
