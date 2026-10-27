# Workspace Invitation Repository Tests Design

## Intent

Add repository integration coverage for creating a workspace invitation with
`PENDING` status and reading it back through `findById()`.

## Scope

- Add Lombok getters, setters, and builder support to `WorkspaceInvitation`.
- Use the existing `@SpringBootTest` and H2 `test` profile.
- Persist a workspace before persisting an invitation because the entity
  relationship has no cascade configuration.
- Set all invitation fields required by the production migration, including
  timestamps and the invitation status.

## Non-goals

- No repository API changes.
- No service/controller implementation.
- No changes to Flyway migrations or production behavior.

## Approach

Keep the existing repository-test style instead of introducing a new
`@DataJpaTest` slice. The tests will use real JPA persistence and assert the
stored status and the result of `findById()`.
