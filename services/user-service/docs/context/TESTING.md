# user-service testing

## Existing tests

| Test | Scope |
| --- | --- |
| `src/test/java/com/notespace/userservice/service/AuthServiceTest.java` | Mockito unit tests for registration success/duplicates/password encoding, login success/normalization/failures, logout delegation, and refresh success/failure. |
| `src/test/java/com/notespace/userservice/service/RefreshTokenServiceTest.java` | Mockito tests for creation, rotation, expiry/revocation/missing-token rejection, and logout revocation. |
| `src/test/java/com/notespace/userservice/service/UserServiceTest.java` | Mockito tests for current-user mapping and missing-user handling. |
| `src/test/java/com/notespace/userservice/security/` | Unit tests for JWT generation/validation, bearer-token authentication, and user-details mapping. |
| `src/test/java/com/notespace/userservice/controller/` | Unit tests for auth, current-user, and admin controller responses/delegation. |
| `src/test/java/com/notespace/userservice/exception/GlobalExceptionHandlerTest.java` | Unit tests for authentication and duplicate-resource error responses. |
| `src/test/java/com/notespace/userservice/UserServiceApplicationTests.java` | `@SpringBootTest` context-load test. |

Stack: JUnit Jupiter, Mockito via Spring Boot test starters, Spring Boot test utilities. Test dependencies are declared in `pom.xml`.

## Commands

From `services/user-service/`:

```powershell
./mvnw test
```

On Windows PowerShell, use `./mvnw.cmd test`.

The Spring context test needs a reachable PostgreSQL database and `JWT_SECRET`, because application config points to local PostgreSQL and resolves that required placeholder. The unit tests mock persistence/security collaborators and do not need a database.

## Gaps

- No controller/API integration tests or database-backed repository/migration tests were found; controllers and security components now have isolated unit tests.
- Context-load test does not itself define a test database or JWT secret.
- Tests are service-focused and do not establish coverage percentages.
- Token hashing is exercised indirectly through mocked repository lookup; raw persistence and migration behavior lack integration verification.
