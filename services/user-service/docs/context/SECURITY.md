# user-service security

All claims here are limited to `security/`, auth services/controllers, and configuration in this repository.

## Configuration and route policy

- `SecurityConfig` disables CSRF, form login, and HTTP Basic; installs the bearer JWT filter before `UsernamePasswordAuthenticationFilter` (no explicit session-creation policy is configured).
- `/api/v1/auth/**` and `/actuator/**` are permitted by HTTP authorization. Other requests require authentication.
- `SecurityConfig` enables method security; `AdminController` protects `GET /api/v1/admintets` with `hasRole('ADMIN')`.
- Authentication failures produce HTTP 401; denied access produces HTTP 403 via Spring Security handlers.
- Actuator web exposure is configured for `health,info`; auth config permits the broader `/actuator/**` matcher.

Sources: `src/main/java/com/notespace/userservice/security/SecurityConfig.java`, `controller/AdminController.java`, `src/main/resources/application.yaml`.

## Credentials and access tokens

- `PasswordEncoder` is `BCryptPasswordEncoder`; registration stores the encoded value in `users.password_hash`.
- Login normalizes email using trim + lowercase (`Locale.ROOT`), authenticates via `AuthenticationManager`, and issues access JWT plus refresh token.
- `JwtService` signs HMAC JWTs with required `jwt.secret` (`JWT_SECRET`) and sets subject to email. Access lifetime is hard-coded to 15 minutes.
- `JwtAuthFilter` accepts `Authorization: Bearer ...`, validates the signed/expiring JWT, loads the current user by email and sets Spring `SecurityContext` authentication/authorities.
- Invalid bearer JWT is ignored by the filter; a protected request then proceeds unauthenticated and is rejected by route authorization.

## Refresh and logout

- Refresh tokens are random UUID strings; only SHA-256 hashes are persisted. Configured expiry is `jwt.refreshExpirationMs` (30 days in YAML).
- `/refresh` checks token existence, revoked flag, and expiry, revokes the old token, and creates a replacement.
- `/logout` revokes the submitted refresh token. Both routes are public, and logout does not invalidate an already issued access JWT.
- No refresh-token reuse detection beyond rejecting a token already marked revoked is implemented; no scheduled cleanup is present.

## Planned / absent

There is no account lockout, email verification, password reset, refresh-cookie transport, role administration endpoint, or separately configured session policy in the inspected code. These are not implied by Spring dependencies or roadmap labels. No actual secret is included in this page.
