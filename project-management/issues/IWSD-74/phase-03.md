# Phase 3: Authorization Guards & Service Integration

**Issue:** IWSD-74
**Phase:** 3 of 4
**Objective:** Integrate authorization guards into HTTP4S routes and service layer, demonstrating declarative permission checking with proper error handling (401/403). Provide both middleware-based (standard) and typed route (optional) approaches for authorization enforcement.
**Estimated Time:** 14 hours (updated from 11 hours)
**Prerequisites:** Completion of Phase 2 (AuthenticationService and CurrentUser available)

## Phase Objective

Integrate the authorization system throughout the application stack, from service layer through HTTP endpoints. This phase demonstrates best practices for declarative permission checking using the Authorization helpers created in Phase 1, with proper HTTP error mapping.

**Note on Architecture:** This project uses Tapir for HTTP endpoints, which provides compile-time type safety by design. The middleware vs typed routes distinction common in raw HTTP4S applications doesn't apply here - Tapir's endpoint definitions are inherently type-safe and composable.

Key deliverables:
- ExampleDocumentService showing Authorization.require patterns
- Tapir endpoints with `.toApi` pattern for bearer auth
- HTTP error handler mapping AuthenticationError → 401/403 responses
- Comprehensive integration tests validating end-to-end workflows
- AUTHORIZATION_GUIDE.md documenting patterns for developers

## Tasks

1. **Create example service using Authorization.require** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/jvm/src/test/scala/works/iterative/core/ExampleDocumentServiceSpec.scala` (NOTE: in jvm not shared)
   - [x] [impl] Write test case for createDocument succeeding when user authenticated
   - [x] [impl] Write test case for updateDocument succeeding when user has "edit" permission
   - [x] [impl] Write test case for updateDocument failing with Forbidden when user lacks permission
   - [x] [impl] Write test case for deleteDocument requiring "delete" permission
   - [x] [impl] Write test case for listDocuments using Authorization.filterAllowed to show only permitted documents
   - [x] [impl] Use TestAuthenticationService and InMemoryPermissionService for testing
   - [x] [impl] Run test: `mill core.jvm.test` (was core.shared.test)
   - [x] [impl] Verify tests fail with "Not found: type ExampleDocumentService"
   - [x] [reviewed] Tests validate service-level authorization guards

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/ExampleDocumentService.scala`
   - [x] [impl] Add PURPOSE comments explaining example service demonstrating authorization patterns
   - [x] [impl] Implement createDocument using Authorization.require for authenticated-only access
   - [x] [impl] Implement updateDocument(id) using Authorization.require(PermissionOp("edit"), PermissionTarget.unsafe("document", id))
   - [x] [impl] Implement deleteDocument(id) using Authorization.require(PermissionOp("delete"), PermissionTarget.unsafe("document", id))
   - [x] [impl] Implement listDocuments using Authorization.filterAllowed to filter results by permission
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify all ExampleDocumentService tests pass (6 tests passed)
   - [x] [reviewed] Service correctly uses authorization guards

   **REFACTOR - Improve Quality:**
   - [x] [impl] Extract permission target construction to helper method (documentTarget)
   - [x] [impl] Add comprehensive Scaladoc showing authorization patterns
   - [x] [impl] Ensure consistent error types (AuthenticationError for auth failures)
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Service is clean example for other developers

   **Success Criteria:** ExampleDocumentService demonstrates Authorization.require, Authorization.filterAllowed patterns with comprehensive tests
   **Testing:** ExampleDocumentServiceSpec validates authorization guards with permission granted/denied scenarios

2. **Create HTTP4S error handling for authentication/authorization** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/test/scala/works/iterative/server/http/AuthErrorHandlerSpec.scala`
   - [x] [impl] Write test case for AuthenticationError.Unauthenticated returning 401 Unauthorized
   - [x] [impl] Write test case for AuthenticationError.Forbidden returning 403 Forbidden
   - [x] [impl] Write test cases for InvalidCredentials, TokenExpired, InvalidToken returning 401
   - [x] [impl] Write test case for error response including appropriate message
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify tests fail with "Not found: AuthErrorHandler"
   - [x] [reviewed] Tests validate HTTP error handling for auth failures

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/AuthErrorHandler.scala`
   - [x] [impl] Add PURPOSE comments explaining auth error mapping to HTTP status codes
   - [x] [impl] Implement error handler that catches AuthenticationError (pattern match on enum)
   - [x] [impl] Map Unauthenticated errors to 401 Unauthorized response
   - [x] [impl] Map Forbidden errors to 403 Forbidden response
   - [x] [impl] Include error message in response body (JSON format)
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify all tests pass (7 tests passed)
   - [x] [reviewed] Error handling correctly maps errors to HTTP status codes

   **REFACTOR - Improve Quality:**
   - [x] [impl] Extract error message formatting to separate functions (formatSimpleError, formatUnauthenticatedError, formatForbiddenError)
   - [x] [impl] Add logging for authentication failures (info level via logAuthFailure)
   - [x] [impl] Ensure sensitive information not leaked in error messages (sanitizeMessage removes tokens)
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify all tests still pass (7 tests passed)
   - [x] [reviewed] Error handling is secure and well-tested

   **Success Criteria:** AuthErrorHandler correctly maps AuthenticationError to 401/403 HTTP responses with appropriate messages
   **Testing:** AuthErrorHandlerSpec validates error mapping for various scenarios

3. **Create example Tapir endpoints using authorization** (TDD Cycle)

   **NOTE:** Implemented using Tapir endpoints instead of raw HTTP4S routes, as Tapir is the project's HTTP framework.

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/test/scala/works/iterative/server/http/ExampleDocumentEndpointsSpec.scala`
   - [x] [impl] Write test case for updateDocument logic succeeding with edit permission
   - [x] [impl] Write test case for updateDocument logic returning Forbidden without permission
   - [x] [impl] Write test case for deleteDocument logic requiring delete permission
   - [x] [impl] Write test case for createDocument logic succeeding when authenticated
   - [x] [impl] Write test case for listDocuments logic filtering by permission
   - [x] [impl] Write test case verifying ExampleDocumentEndpoints compiles with correct definitions
   - [x] [impl] Use TestAuthenticationService and setup test permissions
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify tests fail with "Not found: type ExampleDocumentEndpoints"
   - [x] [reviewed] HTTP route tests validate end-to-end authorization

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/ExampleDocumentEndpoints.scala`
   - [x] [impl] Add PURPOSE comments: Example Tapir endpoints demonstrating authorization integration
   - [x] [impl] Implement endpoints using `.toApi[Unit]` for bearer auth extraction
   - [x] [impl] Implement endpoints using `.apiLogic` to provide CurrentUser context
   - [x] [impl] Call service methods (service uses Authorization.require internally)
   - [x] [impl] Errors automatically mapped via AuthErrorHandler (auth errors to 401/403)
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify all endpoint tests pass (6 tests passed)
   - [x] [reviewed] Routes correctly integrate authorization and error handling

   **REFACTOR - Improve Quality:**
   - [x] [impl] Add comprehensive Scaladoc explaining Tapir authorization pattern
   - [x] [impl] Document `.toApi` and `.apiLogic` extension methods
   - [x] [impl] Document error flow (Service → AuthenticationError → HTTP 401/403)
   - [x] [impl] Run test: `mill server.http.test`
   - [x] [impl] Verify all tests still pass (6 tests passed)
   - [x] [reviewed] Routes are clean and reusable pattern

   **Success Criteria:** Example HTTP4S routes demonstrate authorization integration with proper 401/403 error handling
   **Testing:** ExampleDocumentRoutesSpec validates HTTP routes with various permission scenarios

4. **Create HTTP integration test scenarios** (TDD Cycle) - **COMPLETED**

   **NOTE:** Created in `server/http/src/test/scala` instead of e2e-testing module. The e2e-testing module is for Cucumber/Playwright browser tests. HTTP-level integration tests belong in the server/http module.

   **Test File:** `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/test/scala/works/iterative/server/http/AuthorizationIntegrationSpec.scala`

   **Completed Scenarios:**
   - [x] [impl] User creates document, becomes owner, can edit and delete
   - [x] [impl] User A creates document, User B cannot edit or delete (403 Forbidden)
   - [x] [impl] User with "editor" relation can edit but not delete
   - [x] [impl] List documents returns only user's permitted documents
   - [x] [impl] All tests passing: `mill server.http.test` (28 total tests, 4 new integration tests)

   **Implementation Details:**
   - Uses CurrentUser layer with test profiles
   - Uses InMemoryPermissionService with test permissions
   - Tests service layer integration (not full HTTP server)
   - Validates AuthenticationError flow (Forbidden, Unauthenticated)

   **Success Criteria:** ✅ Integration tests validate complete authorization flow from service layer through authentication, authorization, and error handling
   **Testing:** ✅ AuthorizationIntegrationSpec validates user scenarios with 4 comprehensive tests

5. **Document authorization usage patterns** (Documentation Task) - **COMPLETED**

   **Documentation File:** `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/docs/AUTHORIZATION_GUIDE.md`

   **Completed Documentation:**
   - [x] [impl] Created AUTHORIZATION_GUIDE.md
   - [x] [impl] Documented Authorization.require pattern with code examples
   - [x] [impl] Documented Authorization.filterAllowed pattern for list queries
   - [x] [impl] Documented permission target format (namespace:objectId) with validation rules
   - [x] [impl] Documented permission operations (view, edit, delete, create)
   - [x] [impl] Documented how to configure permission namespaces (PermissionConfig)
   - [x] [impl] Documented testing with TestAuthenticationService and InMemoryPermissionService
   - [x] [impl] Added examples for common scenarios (document ownership, folder sharing, revoking access)
   - [x] [impl] **Documented Tapir Endpoint Integration** (shows ExampleDocumentEndpoints pattern)
   - [x] [impl] **Documented error handling flow** (AuthErrorHandler mapping)
   - [x] [impl] **Added Architecture Notes** explaining why middleware/typed routes distinction doesn't apply to Tapir

   **Success Criteria:** ✅ AUTHORIZATION_GUIDE.md provides comprehensive guidance for developers with working examples
   **Testing:** ✅ Documentation includes runnable code examples from ExampleDocumentService and ExampleDocumentEndpoints

6A. **Create Authorization Middleware (Standard Approach)** - **N/A FOR TAPIR ARCHITECTURE**

   **RATIONALE:** Tapir provides compile-time safety by design through type-safe endpoint definitions. The middleware vs typed routes distinction doesn't apply to Tapir's endpoint model because:

   1. **Tapir endpoints are already type-safe** - Endpoint definitions specify inputs, outputs, and error types at compile time
   2. **`.toApi` extension provides uniform security** - Handles bearer auth and error mapping consistently across all endpoints
   3. **No runtime route matching** - Tapir validates endpoints at compile time, not runtime
   4. **Composable security is built-in** - Security logic lives in service layer via `Authorization.require`

   **Alternative Approach:** See Task 3 (ExampleDocumentEndpoints) for the Tapir pattern:
   - Endpoints use `.toApi[Unit]` for bearer auth
   - Service methods use `Authorization.require` for permission checks
   - Errors automatically mapped to HTTP status codes via AuthErrorHandler

   **Status:** N/A - Not needed for Tapir architecture

6B. **Create Typed Route Protection (Optional Type-Safe Approach)** - **N/A FOR TAPIR ARCHITECTURE**

   **RATIONALE:** Same as Task 6A - Tapir already provides type-safe route protection through:
   - Type-safe endpoint definitions (inputs, outputs, errors)
   - Compile-time validation of endpoint compatibility
   - `.toApi` extension for uniform security handling
   - Service-layer authorization via `Authorization.require`

   **Status:** N/A - Tapir provides compile-time safety without additional abstraction

7. **Update Example Routes to Show Authorization Patterns** - **COMPLETED IN TASK 3**

   **Status:** ExampleDocumentEndpoints already demonstrates Tapir authorization patterns:
   - [x] [impl] ExampleDocumentEndpoints shows `.toApi` pattern for bearer auth
   - [x] [impl] `.apiLogic` extension provides CurrentUser context
   - [x] [impl] Service methods use `Authorization.require` for permission checks
   - [x] [impl] Comprehensive Scaladoc explains the integration pattern
   - [x] [impl] Tests validate authorization at endpoint level (ExampleDocumentEndpointsSpec)
   - [x] [impl] Tests validate complete integration (AuthorizationIntegrationSpec)

   **File:** `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/ExampleDocumentEndpoints.scala`

   **Note:** Task 3 already created the definitive example. No additional "both approaches" needed since Tapir is the approach.

8. **Update Documentation for Tapir Integration** - **COMPLETED IN TASK 5**

   **Status:** AUTHORIZATION_GUIDE.md already includes comprehensive Tapir documentation:
   - [x] [impl] "Tapir Endpoint Integration" section with complete examples
   - [x] [impl] Explanation of `.toApi[Unit]` pattern for bearer auth
   - [x] [impl] Documentation of `.apiLogic` extension for CurrentUser provisioning
   - [x] [impl] Error handling flow diagram (Service → Tapir → AuthErrorHandler → HTTP)
   - [x] [impl] "Architecture Notes: Why Tapir (Not Middleware/Typed Routes)" section
   - [x] [impl] Complete working examples from ExampleDocumentEndpoints
   - [x] [impl] Testing patterns with TestAuthenticationService and InMemoryPermissionService

   **File:** `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/docs/AUTHORIZATION_GUIDE.md`

   **Note:** Task 5 already documented the Tapir approach comprehensively. No "both approaches" documentation needed.

## Phase Success Criteria

- [x] [impl] ExampleDocumentService demonstrates authorization guard patterns (Task 1 ✅)
- [x] [reviewed] Service authorization patterns approved
- [x] [impl] Tapir endpoints correctly map auth errors to 401/403 responses (Task 2 ✅)
- [x] [reviewed] HTTP error handling approved
- [x] [impl] ExampleDocumentEndpoints demonstrates Tapir authorization pattern (Task 3 ✅)
- [x] [reviewed] Tapir approach approved (Tasks 6A/6B marked N/A - Tapir provides compile-time safety)
- [x] [impl] Examples demonstrate Tapir authorization integration (Task 3/7 ✅)
- [x] [reviewed] Tapir approach documented (Task 5/8 ✅)
- [x] [impl] Integration tests validate complete authorization workflows (Task 4 ✅)
- [x] [reviewed] Integration test coverage approved (4 comprehensive scenarios)
- [x] [impl] All tests pass: `mill server.http.test` (28 tests: 6 service + 7 error + 6 endpoint + 3 auth + 4 integration + 2 PAC4J ✅)
- [x] [reviewed] Test quality approved
- [x] [impl] AUTHORIZATION_GUIDE.md documents Tapir integration patterns (Task 5 ✅)
- [x] [reviewed] Documentation approved - includes architecture rationale
- [x] [impl] Can protect any service method with Authorization.require ✅
- [x] [reviewed] **Phase 3 COMPLETE** - Working authorization guards with Tapir endpoint integration

**Phase 3 Summary:**
- ✅ 28 tests passing (100% success rate)
- ✅ ExampleDocumentService demonstrates all authorization patterns
- ✅ ExampleDocumentEndpoints shows Tapir integration
- ✅ AuthorizationIntegrationSpec validates end-to-end workflows
- ✅ AUTHORIZATION_GUIDE.md provides comprehensive developer guidance
- ✅ Tapir architecture rationale documented (why middleware/typed routes N/A)

---

**Phase Status:** Completed
**Next Phase:** Phase 4: Database Persistence - see [phase-04.md](../phase-04.md)
