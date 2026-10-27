# Workspace MVP verification

Completed 2026-10-28, on branch feat/workspace-service-setup. Java 21.0.9, Maven 3.9.16, existing Spring Boot 4.1.1 dependencies retained.

## Observed results

- Ordinary Maven test suite: **40 tests, 0 failures, 0 errors, 0 skipped**. H2 only, Flyway disabled as before.
- PostgreSQL opt-in suite: **9 tests, 0 failures, 0 errors, 0 skipped**. Local PostgreSQL 18.1; isolated cluster under target/mvp-pgdata, loopback port 55439, disposable workspace_mvp_test database. Docker daemon unavailable; Compose's PostgreSQL 17 image was not run.
- Clean-schema run: Flyway applied the unchanged V1 migration, Hibernate ddl-auto=validate succeeded. Repeat run confirmed migration history and validation. UUID generation and all entity mappings round-tripped. Membership queries, uniqueness, role/status checks, invalid names and local workspace FKs passed.
- Maven package succeeded, producing executable target/workspace-service-0.0.1-SNAPSHOT.jar.
- Real HTTP against that JAR with PostgreSQL and local profile: POST 201, list 200, get 200, non-member 404, missing identity 401; surrounding name spaces normalized. Port 18082.
- Spring proxy rollback test flushes a real workspace SQL INSERT, observes it inside the transaction, injects a membership persistence failure and verifies zero rows after rollback. Existing create and rollback tests also pass.
- Profiles tested: explicit test/local allows valid development identity; default, prod, and production overlapping local/test deny it. Missing, malformed, shortened and duplicate UUID headers and unverified bearer tokens return 401. JSON owner spoof cannot change OWNER membership.
- Independent read-only review: no Critical/Important findings. Framework status preservation fix was reproduced as a failing regression test (405 expected, 500 actual), fixed, then included in the passing suite. Raw DTO max length 100 remains documented; canonical service validation is independent.
- git diff --check passed. No user-service modifications, dependency version changes or V1 changes. Initial root documentation was stale and has been reconciled.

## Commands and evidence

From workspace-service with JAVA_HOME pointing to Java 21:

```powershell
.\mvnw.cmd test
$env:WORKSPACE_POSTGRES_TEST_URL='jdbc:postgresql://127.0.0.1:55439/workspace_mvp_test'
$env:WORKSPACE_POSTGRES_TEST_USER='workspace_test'
.\mvnw.cmd '-Dtest=WorkspacePostgresIT' package
```

Actual commands used the locally installed Maven 3.9.16 executable and repository-local Maven cache. Logs: target/final-h2.log, target/postgres-verification.log (clean migration), target/final-postgres-package.log, target/http-smoke.log and target/http-smoke-results.json. These are local ignored build artifacts.

## Git and remaining scope

Commit 976128d: persistence alignment, membership repository queries and repository tests. Application/REST changes remain in the working tree to avoid committing pre-existing untracked create/provider/tests. Documentation is updated on disk but is ignored by the repository's existing Markdown/docs rules; it was not force-added. Do not assume HEAD alone contains the finished MVP.

Subsequent Git finalization (2026-10-28), explicitly authorized by the user: commit c36456d includes the application/REST implementation and tests. The blanket documentation ignore rules have been replaced with explicit secret/artifact exclusions, and project documentation is now included in Git. The preceding paragraph records the earlier MVP verification snapshot, not the finalized Git state. The ordinary suite was rerun during finalization: 40 passed, 0 failures/errors/skipped.

Production JWT integration, invitation API/lifecycle, full permissions, workspace mutation, documents and realtime remain PLANNED. Default/production intentionally remains unavailable without a trusted identity provider. MVP verification does not establish production readiness.
