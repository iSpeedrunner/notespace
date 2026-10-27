# user-service REST API

Base URL in local config: `http://localhost:8081`. All route declarations below are from `controller/`; security is configured separately in `security/SecurityConfig.java`.

| Method | Path | Auth | Request | Response | Controller | Service |
| --- | --- | --- | --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | Public | `RegisterRequest` | 201 `UserResponse` | `AuthController.register` | `AuthService.register` |
| POST | `/api/v1/auth/login` | Public | `LoginRequest` | 200 `LoginResponse` (`accessToken`, `refreshToken`) | `AuthController.login` | `AuthService.login` |
| POST | `/api/v1/auth/refresh` | Public | `RefreshTokenRequest` | 200 `LoginResponse` | `AuthController.refresh` | `AuthService.refreshToken` |
| POST | `/api/v1/auth/logout` | Public | `LogoutRequest` | 204 empty | `AuthController.logout` | `AuthService.logout` |
| GET | `/api/v1/users/me` | Authenticated bearer token | None | 200 `UserResponse` | `UserController.getCurrentUser` | `UserService.getCurrentUser` |
| GET | `/api/v1/admintets` | `ROLE_ADMIN` via method security | None | 200 string `Hello admin` | `AdminController.isAdmin` | None |

`/api/v1/admintets` spelling is the literal current mapping. Do not silently correct the public path as part of documentation; see [[KNOWN-ISSUES]].

## Request validation and errors

`@Valid` is applied to auth request bodies. Request-field rules live in `dto/auth/{RegisterRequest,LoginRequest,RefreshTokenRequest,LogoutRequest}.java`. `GlobalExceptionHandler` returns `ErrorResponse` with timestamp, status, code, message, path, and optional field errors. It maps validation to 400; duplicate email/username to 409; invalid credentials and invalid refresh token to 401. Security entry point/denied handler return bare 401/403 errors through `sendError`, not this JSON DTO. Other exceptions have no explicit mapping here.

## Registration execution

```text
POST /api/v1/auth/register
  → AuthController.register(@Valid RegisterRequest)
  → AuthService.register
  → AuthRepository.existsByEmail / existsByUsername
  → PasswordEncoder.encode; AuthRepository.save(User)
  → UserResponse; HTTP 201
```

See [[FLOWS]] for token and current-user paths, [[SECURITY]] for authorization.
