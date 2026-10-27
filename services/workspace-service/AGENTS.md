# workspace-service

Read README.md and docs/context/INDEX.md before changes. Keep the existing package-by-layer structure and Java 21 / Boot dependency versions unless a requirement demands otherwise.

- This service owns workspace_db, workspace, workspace_member, workspace_invitation. User references are opaque UUIDs; never query user_db.
- Create workspace and OWNER membership in one application transaction. Membership must filter database reads; missing and inaccessible workspaces both return 404.
- CurrentUserProvider is the identity port. Header adapter is local/test only; default/prod/production must fail closed without a trusted provider. Never introduce fake users or unverified JWT authentication.
- Preserve existing Flyway migrations; use the next migration for schema changes.
- Ordinary tests use H2. PostgreSQL tests require a separate disposable database and explicit opt-in.
- Invitation persistence is implemented; invitation lifecycle/API, document operations and production JWT remain planned.
- Update local and system context when API, security, ownership or invariants change.
