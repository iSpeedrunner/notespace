# Implemented user-service flows

## Registration

```text
POST /api/v1/auth/register
 → AuthController.register(@Valid)
 → AuthService.register
 → AuthRepository.existsByEmail, existsByUsername
 → PasswordEncoder.encode; AuthRepository.save(User with Role.USER)
 → UserResponse; HTTP 201
```

Duplicates raise `EmailAlreadyExistsException` or `UsernameAlreadyExistsException`; `GlobalExceptionHandler` maps each to 409. DTO validation errors map to 400. Source: `controller/AuthController.java`, `service/AuthService.java`, `exception/GlobalExceptionHandler.java`.

## Login and token issuance

```text
POST /api/v1/auth/login
 → AuthController.login(@Valid)
 → AuthService.login (trim/lowercase email)
 → AuthenticationManager.authenticate
 → AuthRepository.findByEmail
 → JwtService.generateToken (15-minute access token)
 → RefreshTokenService.createRefreshToken (30-day configured lifetime; persist SHA-256 hash)
 → LoginResponse; HTTP 200
```

Bad credentials are converted to `InvalidCredentialsException` and 401. Source: `service/AuthService.java`, `security/CustomUserDetailsService.java`, `security/JwtService.java`.

## Protected current-user request

```text
GET /api/v1/users/me + Authorization: Bearer <JWT>
 → SecurityConfig filter chain
 → JwtAuthFilter validates token, loads UserDetails, sets SecurityContext
 → UserController.getCurrentUser(Authentication)
 → UserService.getCurrentUser(authentication.getName())
 → AuthRepository.findByEmail; map UserResponse
```

No token or invalid token results in rejection at protected route authorization. Source: `security/JwtAuthFilter.java`, `controller/UserController.java`, `service/UserService.java`.

## Refresh rotation and logout

`POST /api/v1/auth/refresh` hashes the submitted raw token, finds it with owner, rejects missing/revoked/expired tokens, revokes the old record, stores a new hash, and returns a new access/refresh pair. `POST /api/v1/auth/logout` hashes the provided token, looks it up, marks it revoked, and returns 204. Both endpoints are public under `/api/v1/auth/**`; neither revokes existing access JWTs. See [[SECURITY]] and [[DATABASE]].

No workspace, document, realtime, or notification execution flow exists yet.
