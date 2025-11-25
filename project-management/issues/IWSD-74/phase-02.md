# Phase 2: Authentication Integration (Pac4J & Test Mode)

**Issue:** IWSD-74
**Phase:** 2 of 4
**Objective:** Bridge Pac4J OIDC integration with AuthenticationService interface and create test authentication mode for rapid development without OIDC dependencies.
**Estimated Time:** 10 hours (updated from 9 hours)
**Prerequisites:** Completion of Phase 1 (Authorization helpers exist for integration testing)

## Phase Objective

Integrate external authentication (Pac4J for OIDC) with the internal authentication abstractions, and create a test authentication mode for development. This phase enables:

1. **Production OIDC Authentication**: Pac4J handles OpenID Connect authentication, maps profiles to our domain model
2. **Development Test Mode**: Switch users without OIDC configuration, enabling rapid TDD cycles
3. **Environment-Based Selection**: Configuration determines which authentication provider loads
4. **Proper FiberRef Lifecycle**: User context properly scoped to requests, no leaks between requests

Key deliverables:
- Pac4jAuthenticationAdapter bridges Java library to ZIO service
- TestAuthenticationService for controllable user switching
- AuthenticationServiceFactory for environment-based provider selection using Scala 3 enum
- FiberRef lifecycle management ensuring request isolation

## Tasks

1. **Create Pac4jAuthenticationAdapter** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/jvm/src/test/scala/works/iterative/server/http/impl/pac4j/Pac4jAuthenticationAdapterSpec.scala`
   - [x] [impl] Write test case for mapping Pac4J CommonProfile to BasicProfile (id, name, email)
   - [x] [impl] Write test case for handling missing email attribute (should fail or use default)
   - [x] [impl] Write test case for extracting roles from Pac4J profile attributes
   - [x] [impl] Write test case for provideCurrentUser storing user in FiberRef
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify tests fail with "object Pac4jAuthenticationAdapter not found"
   - [x] [reviewed] Tests validate Pac4J profile mapping correctly

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/impl/pac4j/Pac4jAuthenticationAdapter.scala`
   - [x] [impl] Add PURPOSE comments explaining adapter bridges Pac4J Java library to ZIO AuthenticationService
   - [x] [impl] Implement class extending AuthenticationService
   - [x] [impl] Implement method to map CommonProfile to BasicProfile (handle null values defensively with Option)
   - [x] [impl] Implement loggedIn method to extract profile from Pac4J and call provideCurrentUser
   - [x] [impl] Use FiberRefAuthentication for provideCurrentUser implementation
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify all Pac4jAuthenticationAdapter tests pass
   - [x] [reviewed] Profile mapping handles all edge cases correctly

   **REFACTOR - Improve Quality:**
   - [x] [impl] Extract profile mapping to separate pure function for testability
   - [x] [impl] Add comprehensive error handling for malformed profiles
   - [x] [impl] Add logging for profile mapping (debug level, include profile ID)
   - [x] [impl] Add Scaladoc with examples of Pac4J integration
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Code is maintainable and well-documented

   **Success Criteria:** Pac4jAuthenticationAdapter maps Pac4J CommonProfile to BasicProfile and stores in CurrentUser via FiberRef
   **Testing:** Pac4jAuthenticationAdapterSpec validates profile mapping with various Pac4J profile types

2. **Integrate Pac4jAuthenticationAdapter with Pac4jModuleRegistry** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create integration test: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/test/scala/works/iterative/server/http/impl/pac4j/Pac4jIntegrationSpec.scala`
   - [x] [impl] Write test case for Pac4J middleware calling AuthenticationService.loggedIn after successful auth
   - [x] [impl] Write test case verifying CurrentUser available in subsequent ZIO effects
   - [x] [impl] Write test case for FiberRef isolation (concurrent requests don't share user context)
   - [x] [impl] Write test case for FiberRef lifecycle management (cleanup after request)
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify tests fail because Pac4jModuleRegistry doesn't wire adapter yet
   - [x] [reviewed] Integration tests validate end-to-end authentication flow

   **GREEN - Make Test Pass:**
   - [x] [impl] Modify file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/impl/pac4j/Pac4jModuleRegistry.scala`
   - [x] [impl] Add Pac4jAuthenticationAdapter initialization
   - [x] [impl] **IMPORTANT: Use ZIO.scoped for FiberRef lifecycle:**
     ```scala
     ZIO.scoped {
       for {
         fiberRef <- FiberRef.make[Option[User]](None)
         _ <- fiberRef.set(Some(user))
         result <- effect.provideSomeLayer(ZLayer.succeed(CurrentUserLive(fiberRef)))
       } yield result
     }
     ```
   - [x] [impl] Integrate adapter with Pac4J callback handler (call loggedIn after successful authentication)
   - [x] [impl] Ensure adapter is called before HTTP4S routes process request
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify integration tests pass
   - [x] [reviewed] Pac4J integration correctly flows user context to CurrentUser

   **REFACTOR - Improve Quality:**
   - [x] [impl] Review middleware ordering (Pac4J → Auth → Routes)
   - [x] [impl] Add error handling for authentication failures
   - [x] [impl] Add logging at integration points
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Integration is robust and well-tested

   **Success Criteria:** Pac4J successful authentication flows to AuthenticationService.loggedIn, making CurrentUser available throughout request lifecycle with proper FiberRef cleanup
   **Testing:** Pac4jIntegrationSpec validates end-to-end flow with FiberRef isolation

3. **Implement TestAuthenticationService** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/test/scala/works/iterative/core/auth/service/TestAuthenticationServiceSpec.scala`
   - [x] [impl] Write test case for loginAs method setting specific user
   - [x] [impl] Write test case for predefined test users (admin, regular user, viewer)
   - [x] [impl] Write test case verifying CurrentUser reflects loginAs user
   - [x] [impl] Write test case for switching users mid-test (logout, login as different user)
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify tests fail with "object TestAuthenticationService not found"
   - [x] [reviewed] Tests validate test authentication workflows

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/service/TestAuthenticationService.scala`
   - [x] [impl] Add PURPOSE comments explaining test-only authentication for rapid development
   - [x] [impl] Define predefined test users: testAdmin (all roles), testUser (basic roles), testViewer (read-only)
   - [x] [impl] Implement loginAs(userId: String) method creating BasicProfile and calling provideCurrentUser
   - [x] [impl] Implement helper methods: loginAsAdmin, loginAsUser, loginAsViewer
   - [x] [impl] Use FiberRefAuthentication for provideCurrentUser
   - [x] [impl] Create ZLayer factory
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify all tests pass
   - [x] [reviewed] TestAuthenticationService enables easy user switching

   **REFACTOR - Improve Quality:**
   - [x] [impl] Add logout() method for clearing current user
   - [x] [impl] Add warning logging when TestAuthenticationService initialized (should never be in production)
   - [x] [impl] Add comprehensive Scaladoc with testing examples
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Test service is flexible and well-documented

   **Success Criteria:** TestAuthenticationService allows switching users easily without OIDC, with predefined test users for common scenarios
   **Testing:** TestAuthenticationServiceSpec validates user switching and predefined users

4. **Create environment-based authentication service selection** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/test/scala/works/iterative/server/http/AuthenticationServiceFactorySpec.scala`
   - [x] [impl] Write test case for AUTH_PROVIDER=test returning TestAuthenticationService
   - [x] [impl] Write test case for AUTH_PROVIDER=oidc returning Pac4jAuthenticationAdapter
   - [x] [impl] Write test case for AUTH_PROVIDER=test in production environment failing fast (security check)
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify tests fail with "object AuthenticationServiceFactory not found"
   - [x] [reviewed] Tests validate environment-based service selection

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/AuthenticationServiceFactory.scala`
   - [x] [impl] Add PURPOSE comments explaining environment-based authentication provider selection
   - [x] [impl] **SCALA 3 REQUIRED: Define AuthProvider enum:**
     ```scala
     enum AuthProvider:
       case Oidc, Test
     ```
   - [x] [impl] Implement ZLayer factory that reads AUTH_PROVIDER config and parses to enum
   - [x] [impl] Pattern match on AuthProvider.Test => TestAuthenticationService
   - [x] [impl] Pattern match on AuthProvider.Oidc => Pac4jAuthenticationAdapter
   - [x] [impl] Add validation: fail fast if AUTH_PROVIDER=test and ENV=production
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify all tests pass
   - [x] [reviewed] Factory correctly selects authentication service

   **REFACTOR - Improve Quality:**
   - [x] [impl] Add clear error messages for configuration issues
   - [x] [impl] Add logging for which authentication service is loaded
   - [x] [impl] Add Scaladoc explaining configuration options
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Configuration is clear and safe

   **Success Criteria:** AuthenticationServiceFactory selects correct service based on AUTH_PROVIDER config using Scala 3 enum
   **Testing:** AuthenticationServiceFactorySpec validates environment-based selection and security checks

## Phase Success Criteria

- [x] [impl] Pac4jAuthenticationAdapter correctly maps CommonProfile to BasicProfile
- [x] [reviewed] Pac4J integration approved
- [x] [impl] Pac4J successful authentication makes CurrentUser available throughout request
- [x] [reviewed] End-to-end authentication flow approved
- [x] [impl] FiberRef lifecycle properly managed (scoped, no leaks)
- [x] [reviewed] FiberRef usage approved
- [x] [impl] TestAuthenticationService enables user switching without OIDC
- [x] [reviewed] Test authentication service approved
- [x] [impl] AuthenticationServiceFactory selects correct provider based on config using Scala 3 enum
- [x] [reviewed] Environment-based configuration approved
- [x] [impl] All integration tests pass: `mill server.http.test`
- [x] [reviewed] Integration test coverage approved
- [x] [impl] Can login via Pac4J and access CurrentUser in service layer
- [x] [reviewed] Phase validation approved - working authentication integration

---

**Phase Status:** Completed
**Next Phase:** Phase 3: Authorization Guards & Service Integration - see [phase-03.md](../phase-03.md)
