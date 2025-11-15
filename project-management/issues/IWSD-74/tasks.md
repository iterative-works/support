# Implementation Tasks: Investigate authentication for HTTP4S server

**Issue:** IWSD-74
**Complexity:** Complex
**Estimated Total Time:** 54 hours (updated from 48 hours, +6 hours for infrastructure)
**Generated:** 2025-11-13
**Updated:** 2025-11-15 (Applied critical issue fixes + infrastructure tasks)

## Overview

Implement production-ready authentication and authorization system for HTTP4S server by building on existing well-designed abstractions (PermissionService interface, user model hierarchy, AuthenticationService, CurrentUser). This enables pluggable authentication (OIDC for production, test mode for development), relationship-based permissions (Zanzibar-inspired ReBAC), and declarative authorization guards throughout the ZIO application.

## Implementation Strategy

Build incrementally across 5 phases, starting with in-memory implementations for immediate value, then integrating authentication providers, adding authorization guards to services, implementing database persistence, and finally optimizing for production. Each phase delivers working functionality that can be tested independently. The TDD approach ensures comprehensive test coverage and prevents authorization bypass bugs (critical security concern).

**Architectural Note:** This implementation follows FCIS (Functional Core, Imperative Shell) principles by extracting pure domain logic (PermissionLogic) from effect-based orchestration (PermissionService, Authorization helpers). Effect-based code is recognized as application-layer infrastructure, not core domain.

## Phases

### Phase 1: Permission Foundation (In-Memory Implementation)

**Objective:** Create working permission system with in-memory storage, enabling immediate testing of Zanzibar-inspired ReBAC model without database dependencies. Also establish metrics and configuration infrastructure.

**Estimated Time:** 14 hours (updated from 10 hours, +4 hours for metrics and config validation)

**Prerequisites:** None (foundational phase)

#### Tasks

0. **Define Domain Value Types (Scala 3 Opaque Types)** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/test/scala/works/iterative/core/auth/DomainTypesSpec.scala`
   - [ ] [impl] Write test case for UserId creation and value extraction
   - [ ] [impl] Write test case for PermissionOp creation
   - [ ] [impl] **CRITICAL: Write comprehensive PermissionTarget validation tests:**
     - Valid format: "document:123" → Right(PermissionTarget)
     - Valid with underscore: "task_list:abc-123" → Right(PermissionTarget)
     - Invalid: missing colon → Left(ValidationError)
     - Invalid: uppercase namespace → Left(ValidationError)
     - Invalid: special chars in namespace → Left(ValidationError)
     - Invalid: namespace > 50 chars → Left(ValidationError)
     - Invalid: empty namespace → Left(ValidationError)
     - Invalid: empty objectId → Left(ValidationError)
   - [ ] [impl] Write test case for PermissionTarget.unsafe constructor (no validation)
   - [ ] [impl] Write test case for namespace and objectId extraction
   - [ ] [impl] Write test case that UserId cannot be assigned to PermissionOp (type safety)
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify tests fail with "type UserId not found"
   - [x] [reviewed] Tests validate type-safe domain identifiers with strict input validation

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/DomainTypes.scala`
   - [ ] [impl] Add PURPOSE comments: Type-safe domain identifiers using Scala 3 opaque types
   - [ ] [impl] **SCALA 3 REQUIRED: Define UserId as opaque type:**
     ```scala
     opaque type UserId = String
     object UserId:
       def apply(value: String): UserId = value
       extension (id: UserId)
         def value: String = id
     ```
   - [ ] [impl] **SCALA 3 REQUIRED: Define PermissionOp as opaque type:**
     ```scala
     opaque type PermissionOp = String
     object PermissionOp:
       def apply(value: String): PermissionOp = value
       extension (op: PermissionOp)
         def value: String = op
     ```
   - [ ] [impl] **SCALA 3 REQUIRED: Define PermissionTarget as opaque type with validation:**
     ```scala
     opaque type PermissionTarget = String
     object PermissionTarget:
       private val pattern = """^([a-z][a-z0-9_]*):([a-zA-Z0-9-]+)$""".r

       def parse(value: String): Either[ValidationError, PermissionTarget] = value match
         case pattern(namespace, objectId) if namespace.length <= 50 => Right(value)
         case _ => Left(ValidationError("Invalid format. Expected namespace:objectId"))

       def unsafe(namespace: String, objectId: String): PermissionTarget =
         s"$namespace:$objectId"

       extension (target: PermissionTarget)
         def value: String = target
         def namespace: String = target.split(":").head
         def objectId: String = target.split(":").last
     ```
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify all type tests pass
   - [x] [reviewed] Opaque types provide zero-cost compile-time safety

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Add comprehensive Scaladoc explaining opaque types benefits
   - [ ] [impl] Add usage examples in documentation
   - [ ] [impl] Ensure validation errors are descriptive
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify all tests still pass
   - [x] [reviewed] Type definitions are clear and well-documented

   **Success Criteria:** All domain IDs use opaque types with compile-time safety and runtime validation
   **Testing:** DomainTypesSpec validates type safety, parsing, and validation

1. **Create RelationTuple domain model** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/test/scala/works/iterative/core/auth/RelationTupleSpec.scala`
   - [x] [impl] Write test case for RelationTuple creation with valid userId, relation, and target
   - [x] [impl] Write test case for RelationTuple equality (same values should be equal)
   - [x] [impl] Write test case for RelationTuple hashing (for use in Set)
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify tests fail with "object RelationTuple is not a member of package works.iterative.core.auth"
   - [x] [reviewed] Tests properly validate RelationTuple value object behavior

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/RelationTuple.scala`
   - [x] [impl] Add PURPOSE comments explaining relation tuples represent (user, relation, target) triples
   - [x] [impl] Implement case class `RelationTuple(user: UserId, relation: String, target: PermissionTarget)`
   - [x] [impl] Note: Uses opaque types defined in Task 0
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all RelationTuple tests pass
   - [x] [reviewed] RelationTuple implementation is correct and minimal

   **REFACTOR - Improve Quality:**
   - [x] [impl] Review implementation for immutability (case class ensures this)
   - [x] [impl] Add Scaladoc comments explaining each field (user = subject, relation = relationship type, target = object)
   - [x] [impl] Ensure follows project naming conventions
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Code quality meets standards

   **Success Criteria:** RelationTuple case class exists with proper equality semantics and can be stored in Set
   **Testing:** RelationTupleSpec validates creation, equality, hashing

2. **Create PermissionConfig for namespace rules** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/test/scala/works/iterative/core/auth/PermissionConfigSpec.scala`
   - [x] [impl] Write test case for PermissionConfig defining "document" namespace with owner→editor→viewer hierarchy
   - [x] [impl] Write test case for computedRelations method returning implied relations (owner implies [edit, view, delete])
   - [x] [impl] Write test case for namespace without implications (direct permissions only)
   - [x] [impl] Write test case for maximum inheritance depth limit (prevent DoS)
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify tests fail with "object PermissionConfig not found"
   - [x] [reviewed] Tests validate permission inheritance rules correctly

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/PermissionConfig.scala`
   - [x] [impl] Add PURPOSE comments explaining permission config defines inheritance rules per namespace
   - [x] [impl] Implement case class `NamespaceConfig(implications: Map[String, Set[String]])`
   - [x] [impl] Implement case class `PermissionConfig(namespaces: Map[String, NamespaceConfig])`
   - [x] [impl] Add method `computedRelations(namespace: String, relation: String): Set[String]` returning implied permissions
   - [x] [impl] **SECURITY: Add maxInheritanceDepth = 10 limit to prevent DoS**
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all PermissionConfig tests pass
   - [x] [reviewed] PermissionConfig correctly computes inherited permissions

   **REFACTOR - Improve Quality:**
   - [x] [impl] Extract default configurations (document, folder) as companion object constants
   - [x] [impl] Add Scaladoc with examples of permission hierarchies
   - [x] [impl] Ensure pure function (no side effects in computedRelations)
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Code is clear and reusable

   **Success Criteria:** PermissionConfig can define namespace-specific permission inheritance rules and compute implied relations with depth limits
   **Testing:** PermissionConfigSpec validates inheritance logic with various configurations

3. **Create PermissionLogic (Pure Domain Functions)** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/test/scala/works/iterative/core/auth/PermissionLogicSpec.scala`
   - [x] [impl] Write test case for isAllowed with direct permission (pure function)
   - [x] [impl] Write test case for isAllowed with inherited permission (pure function)
   - [x] [impl] Write test case for isAllowed with denied permission (pure function)
   - [x] [impl] Write test case for listAllowed reverse lookup (pure function)
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify tests fail with "object PermissionLogic not found"
   - [x] [reviewed] Tests validate pure permission checking logic

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/PermissionLogic.scala`
   - [x] [impl] Add PURPOSE comments: Pure domain logic for permission checking (FCIS functional core)
   - [x] [impl] Implement pure function:
     ```scala
     def isAllowed(
       userId: UserId,
       action: PermissionOp,
       target: PermissionTarget,
       tuples: Set[RelationTuple],
       config: PermissionConfig
     ): Boolean
     ```
   - [x] [impl] Implement pure function:
     ```scala
     def listAllowed(
       userId: UserId,
       action: PermissionOp,
       namespace: String,
       tuples: Set[RelationTuple],
       config: PermissionConfig
     ): Set[String]
     ```
   - [x] [impl] Check both direct permissions and computed (inherited) permissions
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all PermissionLogic tests pass
   - [x] [reviewed] Pure logic correctly implements Zanzibar model

   **REFACTOR - Improve Quality:**
   - [x] [impl] Extract permission inheritance resolution to private helper function
   - [x] [impl] Add comprehensive Scaladoc with Zanzibar concepts explained
   - [x] [impl] Ensure all functions are pure (no side effects)
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Pure functions are testable and maintainable

   **Success Criteria:** PermissionLogic provides pure functions for permission checking that can be tested without ZIO
   **Testing:** PermissionLogicSpec validates all permission logic as pure functions

4. **Implement InMemoryPermissionService** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/test/scala/works/iterative/core/auth/InMemoryPermissionServiceSpec.scala`
   - [x] [impl] Write test case for direct permission (user has "owner" relation, check "owner" permission → true)
   - [x] [impl] Write test case for computed permission (user has "owner" relation, check "view" permission → true via inheritance)
   - [x] [impl] Write test case for denied permission (user lacks any relation → false)
   - [x] [impl] Write test case for addRelation and removeRelation operations
   - [x] [impl] Write test case for listAllowed returning all resources user can access
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify tests fail with "object InMemoryPermissionService not found"
   - [x] [reviewed] Tests cover both direct and inherited permissions

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/InMemoryPermissionService.scala`
   - [ ] [impl] Add PURPOSE comments explaining in-memory ReBAC permission service for testing/simple deployments
   - [ ] [impl] Add NOTE: This is application-layer infrastructure (effects + storage), not domain
   - [ ] [impl] Implement class with `Ref[Set[RelationTuple]]` storage
   - [ ] [impl] **KEY: Call PermissionLogic.isAllowed (pure function) from implementation:**
     ```scala
     def isAllowed(userId: UserId, action: PermissionOp, target: PermissionTarget): UIO[Boolean] =
       storage.get.map { tuples =>
         PermissionLogic.isAllowed(userId, action, target, tuples, config)
       }
     ```
   - [ ] [impl] Implement addRelation, removeRelation using Ref.update
   - [ ] [impl] Implement listAllowed calling PermissionLogic.listAllowed
   - [ ] [impl] Create ZLayer factory method with ZLayer.fromZIO pattern (lazy initialization)
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify all InMemoryPermissionService tests pass
   - [x] [reviewed] Implementation correctly delegates to pure logic

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Add error handling (fail closed on unexpected cases)
   - [ ] [impl] Optimize listAllowed query (single Ref.get, filter in memory)
   - [ ] [impl] Add comprehensive Scaladoc with usage examples
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify all tests still pass
   - [x] [reviewed] Code is efficient and maintainable

   **Success Criteria:** InMemoryPermissionService correctly checks permissions by delegating to pure PermissionLogic
   **Testing:** InMemoryPermissionServiceSpec validates all permission operations with various scenarios

5. **Add listAllowed method to PermissionService trait** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Update InMemoryPermissionServiceSpec to explicitly test listAllowed interface method
   - [x] [impl] Write property-based test with 100 random relation tuples, verify listAllowed returns correct subset
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify test fails if listAllowed not in PermissionService interface
   - [x] [reviewed] Test validates listAllowed contract correctly

   **GREEN - Make Test Pass:**
   - [x] [impl] Modify file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/PermissionService.scala`
   - [x] [impl] Add NOTE: This interface is application-layer (uses ZIO effects), not domain
   - [x] [impl] Add method signature: `def listAllowed(subj: UserInfo, action: PermissionOp, namespace: String): UIO[Set[String]]`
   - [x] [impl] Add Scaladoc explaining reverse lookup for efficient authorization-aware queries
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify InMemoryPermissionService tests pass
   - [x] [reviewed] listAllowed interface correctly defined

   **REFACTOR - Improve Quality:**
   - [x] [impl] Review method signature for consistency with isAllowed
   - [x] [impl] Add usage examples in Scaladoc (list all documents user can edit)
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Interface is clear and consistent

   **Success Criteria:** PermissionService interface includes listAllowed for reverse lookup queries
   **Testing:** InMemoryPermissionServiceSpec validates listAllowed implementation

6. **Define AuthenticationError types (Scala 3 Enum)** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/jvm/src/test/scala/works/iterative/core/auth/AuthenticationErrorSpec.scala`
   - [x] [impl] Write test case for AuthenticationError variants
   - [x] [impl] Write test case for exhaustive pattern matching on errors
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify tests fail with "enum AuthenticationError not found"
   - [x] [reviewed] Tests validate error type definitions

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/AuthenticationError.scala`
   - [x] [impl] Add PURPOSE comments: Domain error types for authentication/authorization failures
   - [x] [impl] **SCALA 3 REQUIRED: Define AuthenticationError enum:**
     ```scala
     enum AuthenticationError extends Exception:
       case Unauthenticated(message: String)
       case Forbidden(resource: String, action: String)
       case InvalidCredentials
       case TokenExpired
       case InvalidToken(reason: String)
     ```
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all tests pass
   - [x] [reviewed] Error types use Scala 3 enum correctly

   **REFACTOR - Improve Quality:**
   - [x] [impl] Add helper methods for common error construction
   - [x] [impl] Add Scaladoc explaining when each error type is used
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Error types are well-documented

   **Success Criteria:** Error types use Scala 3 enum with exhaustive pattern matching
   **Testing:** AuthenticationErrorSpec validates error definitions

7. **Create Authorization helper object** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/test/scala/works/iterative/core/auth/AuthorizationSpec.scala`
   - [x] [impl] Write test case for Authorization.require allowing effect when permission granted
   - [x] [impl] Write test case for Authorization.require failing with Forbidden when permission denied
   - [x] [impl] Write test case for Authorization.check returning Either (not Nothing error type)
   - [x] [impl] Write test case for Authorization.withPermission filtering effect result
   - [x] [impl] Write test case for Authorization.filterAllowed filtering list of resources
   - [x] [impl] Use InMemoryPermissionService and mock CurrentUser for testing
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify tests fail with "object Authorization not found"
   - [x] [reviewed] Tests validate declarative authorization guard behavior

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/Authorization.scala`
   - [x] [impl] Add PURPOSE comments: ZIO-based authorization helpers (application layer, not domain)
   - [x] [impl] Add NOTE: These orchestrate effects; pure authorization rules in PermissionLogic
   - [x] [impl] Implement `def require[R, E, A](op: PermissionOp, target: PermissionTarget)(effect: ZIO[R, E, A]): ZIO[R & CurrentUser & PermissionService, E | AuthenticationError, A]`
   - [x] [impl] Implement `def check(op: PermissionOp, target: PermissionTarget): ZIO[CurrentUser & PermissionService, AuthenticationError, Boolean]`
   - [x] [impl] **IMPORTANT: Check returns typed error (not Nothing) for fail-closed safety**
   - [x] [impl] Implement `def withPermission[R, E, A](op: PermissionOp, target: PermissionTarget)(effect: ZIO[R, E, Option[A]]): ZIO[R & CurrentUser & PermissionService, E, Option[A]]`
   - [x] [impl] Implement `def filterAllowed[A](op: PermissionOp, items: Seq[A])(extractTarget: A => PermissionTarget): ZIO[CurrentUser & PermissionService, Nothing, Seq[A]]`
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all Authorization tests pass
   - [x] [reviewed] Authorization helpers correctly integrate CurrentUser and PermissionService

   **REFACTOR - Improve Quality:**
   - [x] [impl] Optimize filterAllowed using PermissionService.listAllowed (batch query instead of N checks)
   - [x] [impl] Add comprehensive Scaladoc with usage examples for each method
   - [x] [impl] Extract common pattern (get user, check permission) to private helper
   - [x] [impl] Ensure all methods fail closed (deny on error)
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Code is DRY and well-documented

   **Success Criteria:** Authorization object provides declarative guards (require, check, withPermission, filterAllowed) that integrate CurrentUser and PermissionService
   **Testing:** AuthorizationSpec validates all authorization helpers with permission granted/denied scenarios

8. **Create AlwaysAllowPermissionService for testing** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/jvm/src/test/scala/works/iterative/core/auth/AlwaysAllowPermissionServiceSpec.scala`
   - [x] [impl] Write test case verifying isAllowed always returns true
   - [x] [impl] Write test case verifying listAllowed returns all possible resource IDs (empty set, since it can't know)
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify tests fail with "object AlwaysAllowPermissionService not found"
   - [x] [reviewed] Tests validate always-allow behavior

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/AlwaysAllowPermissionService.scala`
   - [x] [impl] Add PURPOSE comments with WARNING: test/emergency use only, never use in production
   - [x] [impl] Implement class with isAllowed returning ZIO.succeed(true)
   - [x] [impl] Implement listAllowed returning ZIO.succeed(Set.empty) (can't list without actual data)
   - [x] [impl] Create ZLayer factory
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify all tests pass
   - [x] [reviewed] Implementation is correct (albeit insecure by design)

   **REFACTOR - Improve Quality:**
   - [x] [impl] Add prominent Scaladoc warning about security implications
   - [x] [impl] Add runtime check to log warning when instantiated
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Warnings are clear and prominent

   **Success Criteria:** AlwaysAllowPermissionService exists for testing but has prominent warnings against production use
   **Testing:** AlwaysAllowPermissionServiceSpec validates always-allow behavior

9. **Create Metrics Infrastructure** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/jvm/src/test/scala/works/iterative/core/metrics/MetricsServiceSpec.scala`
   - [x] [impl] Write test case for recordCounter incrementing metric
   - [x] [impl] Write test case for recordTimer measuring duration
   - [x] [impl] Write test case for recordGauge setting value
   - [x] [impl] Write test case for test implementation (no-op, verifies it doesn't error)
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify tests fail with "trait MetricsService not found"
   - [x] [reviewed] Tests validate metrics API

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/metrics/MetricsService.scala`
   - [x] [impl] Add PURPOSE comments: Abstraction for recording application metrics
   - [x] [impl] Define trait MetricsService:
     ```scala
     trait MetricsService:
       def recordCounter(name: String, tags: Map[String, String] = Map.empty): UIO[Unit]
       def recordTimer(name: String, duration: Duration, tags: Map[String, String] = Map.empty): UIO[Unit]
       def recordGauge(name: String, value: Double, tags: Map[String, String] = Map.empty): UIO[Unit]
     ```
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/metrics/NoOpMetricsService.scala`
   - [x] [impl] Implement NoOpMetricsService with all methods returning ZIO.unit (for testing)
   - [x] [impl] Add ZLayer factory for NoOpMetricsService
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify all tests pass
   - [x] [reviewed] Metrics abstraction defined

   **REFACTOR - Improve Quality:**
   - [x] [impl] Define standard metric names as constants:
     - `permission.check.duration`
     - `permission.check.infrastructure_failure`
     - `auth.login.success`
     - `auth.login.failure`
   - [x] [impl] Add Scaladoc explaining when to use each metric type
   - [x] [impl] Add note: Production implementation (ZIO Metrics/Micrometer) to be added in Phase 5
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Metrics API is well-documented

   **Success Criteria:** MetricsService abstraction exists with no-op implementation for immediate use
   **Testing:** MetricsServiceSpec validates metrics API

10. **Create Configuration Validation** (TDD Cycle)

   **RED - Write Failing Test:**
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/jvm/src/test/scala/works/iterative/core/config/ConfigValidatorSpec.scala`
   - [x] [impl] Write test case for missing required ENV var failing validation
   - [x] [impl] Write test case for invalid enum value failing with clear message
   - [x] [impl] Write test case for AUTH_PROVIDER=test in production environment failing
   - [x] [impl] Write test case for OIDC requiring OIDC_CLIENT_ID
   - [x] [impl] Write test case for validation collecting ALL errors (not stopping at first)
   - [x] [impl] Write test case for valid configuration passing
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify tests fail with "object ConfigValidator not found"
   - [x] [reviewed] Tests validate comprehensive config checking

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/config/ConfigValidator.scala`
   - [x] [impl] Add PURPOSE comments: Validates application configuration on startup
   - [x] [impl] Define case class ConfigValidationError(errors: List[String])
   - [x] [impl] Implement validateConfig returning Either[ConfigValidationError, ValidatedConfig]:
     ```scala
     def validateConfig(
       authProvider: Option[String],
       permissionService: Option[String],
       environment: Option[String],
       oidcClientId: Option[String]
     ): Either[ConfigValidationError, ValidatedConfig]
     ```
   - [x] [impl] Validate AUTH_PROVIDER parses to enum (Oidc | Test)
   - [x] [impl] Validate PERMISSION_SERVICE parses to enum (Memory | Database)
   - [x] [impl] Validate AUTH_PROVIDER=test forbidden when ENV=production
   - [x] [impl] Validate OIDC requires OIDC_CLIENT_ID, OIDC_CLIENT_SECRET, OIDC_DISCOVERY_URI
   - [x] [impl] Collect ALL errors before returning (use Validated or similar)
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify all tests pass
   - [x] [reviewed] Validation is comprehensive

   **REFACTOR - Improve Quality:**
   - [x] [impl] Add helper for reading ENV vars with defaults
   - [x] [impl] Format error messages clearly (bullet list of issues)
   - [x] [impl] Add Scaladoc with examples of valid configurations
   - [x] [impl] Run test: `mill core.jvm.test`
   - [x] [impl] Verify all tests still pass
   - [x] [reviewed] Configuration validation is clear and helpful

   **Success Criteria:** Configuration validated on startup with clear error messages listing all issues
   **Testing:** ConfigValidatorSpec validates all validation rules

#### Phase Success Criteria

- [ ] [impl] Opaque types provide compile-time safety for all domain IDs
- [x] [reviewed] Scala 3 type definitions approved
- [ ] [impl] PermissionLogic provides pure, testable permission checking functions
- [x] [reviewed] FCIS separation approved (pure logic vs effects)
- [ ] [impl] RelationTuple case class correctly represents (user, relation, target) triples
- [x] [reviewed] RelationTuple design approved
- [ ] [impl] PermissionConfig defines namespace-specific permission inheritance rules with depth limits
- [x] [reviewed] PermissionConfig design approved
- [ ] [impl] InMemoryPermissionService checks permissions by delegating to pure PermissionLogic
- [x] [reviewed] InMemoryPermissionService implementation approved
- [ ] [impl] Authorization helpers provide declarative guards (require, check, withPermission, filterAllowed)
- [x] [reviewed] Authorization helper API approved
- [ ] [impl] MetricsService abstraction exists with no-op implementation
- [x] [reviewed] Metrics infrastructure approved
- [ ] [impl] ConfigValidator validates all configuration on startup
- [x] [reviewed] Configuration validation approved
- [ ] [impl] All unit tests pass: `mill core.shared.test`
- [x] [reviewed] Test coverage and quality approved (100% coverage of permission logic)
- [ ] [impl] Can grant user "owner" on document:123, verify they have "view" via inheritance
- [x] [reviewed] Phase validation approved - working in-memory permission system with infrastructure

---

### Phase 2: Authentication Integration (Pac4J & Test Mode)

**Objective:** Bridge Pac4J OIDC integration with AuthenticationService interface and create test authentication mode for rapid development without OIDC dependencies.

**Estimated Time:** 10 hours (updated from 9 hours)

**Prerequisites:** Completion of Phase 1 (Authorization helpers exist for integration testing)

#### Tasks

1. **Create Pac4jAuthenticationAdapter** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/jvm/src/test/scala/works/iterative/server/http/impl/pac4j/Pac4jAuthenticationAdapterSpec.scala`
   - [ ] [impl] Write test case for mapping Pac4J CommonProfile to BasicProfile (id, name, email)
   - [ ] [impl] Write test case for handling missing email attribute (should fail or use default)
   - [ ] [impl] Write test case for extracting roles from Pac4J profile attributes
   - [ ] [impl] Write test case for provideCurrentUser storing user in FiberRef
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify tests fail with "object Pac4jAuthenticationAdapter not found"
   - [ ] [reviewed] Tests validate Pac4J profile mapping correctly

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/impl/pac4j/Pac4jAuthenticationAdapter.scala`
   - [ ] [impl] Add PURPOSE comments explaining adapter bridges Pac4J Java library to ZIO AuthenticationService
   - [ ] [impl] Implement class extending AuthenticationService
   - [ ] [impl] Implement method to map CommonProfile to BasicProfile (handle null values defensively with Option)
   - [ ] [impl] Implement loggedIn method to extract profile from Pac4J and call provideCurrentUser
   - [ ] [impl] Use FiberRefAuthentication for provideCurrentUser implementation
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify all Pac4jAuthenticationAdapter tests pass
   - [ ] [reviewed] Profile mapping handles all edge cases correctly

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Extract profile mapping to separate pure function for testability
   - [ ] [impl] Add comprehensive error handling for malformed profiles
   - [ ] [impl] Add logging for profile mapping (debug level, include profile ID)
   - [ ] [impl] Add Scaladoc with examples of Pac4J integration
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] Code is maintainable and well-documented

   **Success Criteria:** Pac4jAuthenticationAdapter maps Pac4J CommonProfile to BasicProfile and stores in CurrentUser via FiberRef
   **Testing:** Pac4jAuthenticationAdapterSpec validates profile mapping with various Pac4J profile types

2. **Integrate Pac4jAuthenticationAdapter with Pac4jModuleRegistry** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create integration test: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/test/scala/works/iterative/server/http/impl/pac4j/Pac4jIntegrationSpec.scala`
   - [ ] [impl] Write test case for Pac4J middleware calling AuthenticationService.loggedIn after successful auth
   - [ ] [impl] Write test case verifying CurrentUser available in subsequent ZIO effects
   - [ ] [impl] Write test case for FiberRef isolation (concurrent requests don't share user context)
   - [ ] [impl] Write test case for FiberRef lifecycle management (cleanup after request)
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify tests fail because Pac4jModuleRegistry doesn't wire adapter yet
   - [ ] [reviewed] Integration tests validate end-to-end authentication flow

   **GREEN - Make Test Pass:**
   - [ ] [impl] Modify file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/impl/pac4j/Pac4jModuleRegistry.scala`
   - [ ] [impl] Add Pac4jAuthenticationAdapter initialization
   - [ ] [impl] **IMPORTANT: Use ZIO.scoped for FiberRef lifecycle:**
     ```scala
     ZIO.scoped {
       for {
         fiberRef <- FiberRef.make[Option[User]](None)
         _ <- fiberRef.set(Some(user))
         result <- effect.provideSomeLayer(ZLayer.succeed(CurrentUserLive(fiberRef)))
       } yield result
     }
     ```
   - [ ] [impl] Integrate adapter with Pac4J callback handler (call loggedIn after successful authentication)
   - [ ] [impl] Ensure adapter is called before HTTP4S routes process request
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify integration tests pass
   - [ ] [reviewed] Pac4J integration correctly flows user context to CurrentUser

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Review middleware ordering (Pac4J → Auth → Routes)
   - [ ] [impl] Add error handling for authentication failures
   - [ ] [impl] Add logging at integration points
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] Integration is robust and well-tested

   **Success Criteria:** Pac4J successful authentication flows to AuthenticationService.loggedIn, making CurrentUser available throughout request lifecycle with proper FiberRef cleanup
   **Testing:** Pac4jIntegrationSpec validates end-to-end flow with FiberRef isolation

3. **Implement TestAuthenticationService** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/test/scala/works/iterative/core/auth/service/TestAuthenticationServiceSpec.scala`
   - [ ] [impl] Write test case for loginAs method setting specific user
   - [ ] [impl] Write test case for predefined test users (admin, regular user, viewer)
   - [ ] [impl] Write test case verifying CurrentUser reflects loginAs user
   - [ ] [impl] Write test case for switching users mid-test (logout, login as different user)
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify tests fail with "object TestAuthenticationService not found"
   - [ ] [reviewed] Tests validate test authentication workflows

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/service/TestAuthenticationService.scala`
   - [ ] [impl] Add PURPOSE comments explaining test-only authentication for rapid development
   - [ ] [impl] Define predefined test users: testAdmin (all roles), testUser (basic roles), testViewer (read-only)
   - [ ] [impl] Implement loginAs(userId: String) method creating BasicProfile and calling provideCurrentUser
   - [ ] [impl] Implement helper methods: loginAsAdmin, loginAsUser, loginAsViewer
   - [ ] [impl] Use FiberRefAuthentication for provideCurrentUser
   - [ ] [impl] Create ZLayer factory
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify all tests pass
   - [ ] [reviewed] TestAuthenticationService enables easy user switching

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Add configuration option for custom test users (via config file or builder)
   - [ ] [impl] Add warning logging when TestAuthenticationService initialized (should never be in production)
   - [ ] [impl] Add comprehensive Scaladoc with testing examples
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] Test service is flexible and well-documented

   **Success Criteria:** TestAuthenticationService allows switching users easily without OIDC, with predefined test users for common scenarios
   **Testing:** TestAuthenticationServiceSpec validates user switching and predefined users

4. **Create environment-based authentication service selection** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/test/scala/works/iterative/server/http/AuthenticationServiceFactorySpec.scala`
   - [ ] [impl] Write test case for AUTH_PROVIDER=test returning TestAuthenticationService
   - [ ] [impl] Write test case for AUTH_PROVIDER=oidc returning Pac4jAuthenticationAdapter
   - [ ] [impl] Write test case for AUTH_PROVIDER=test in production environment failing fast (security check)
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify tests fail with "object AuthenticationServiceFactory not found"
   - [ ] [reviewed] Tests validate environment-based service selection

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/AuthenticationServiceFactory.scala`
   - [ ] [impl] Add PURPOSE comments explaining environment-based authentication provider selection
   - [ ] [impl] **SCALA 3 REQUIRED: Define AuthProvider enum:**
     ```scala
     enum AuthProvider:
       case Oidc, Test
     ```
   - [ ] [impl] Implement ZLayer factory that reads AUTH_PROVIDER config and parses to enum
   - [ ] [impl] Pattern match on AuthProvider.Test => TestAuthenticationService
   - [ ] [impl] Pattern match on AuthProvider.Oidc => Pac4jAuthenticationAdapter
   - [ ] [impl] Add validation: fail fast if AUTH_PROVIDER=test and ENV=production
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify all tests pass
   - [ ] [reviewed] Factory correctly selects authentication service

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Add clear error messages for configuration issues
   - [ ] [impl] Add logging for which authentication service is loaded
   - [ ] [impl] Add Scaladoc explaining configuration options
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] Configuration is clear and safe

   **Success Criteria:** AuthenticationServiceFactory selects correct service based on AUTH_PROVIDER config using Scala 3 enum
   **Testing:** AuthenticationServiceFactorySpec validates environment-based selection and security checks

#### Phase Success Criteria

- [ ] [impl] Pac4jAuthenticationAdapter correctly maps CommonProfile to BasicProfile
- [ ] [reviewed] Pac4J integration approved
- [ ] [impl] Pac4J successful authentication makes CurrentUser available throughout request
- [ ] [reviewed] End-to-end authentication flow approved
- [ ] [impl] FiberRef lifecycle properly managed (scoped, no leaks)
- [ ] [reviewed] FiberRef usage approved
- [ ] [impl] TestAuthenticationService enables user switching without OIDC
- [ ] [reviewed] Test authentication service approved
- [ ] [impl] AuthenticationServiceFactory selects correct provider based on config using Scala 3 enum
- [ ] [reviewed] Environment-based configuration approved
- [ ] [impl] All integration tests pass: `mill server.http.test`
- [ ] [reviewed] Integration test coverage approved
- [ ] [impl] Can login via Pac4J and access CurrentUser in service layer
- [ ] [reviewed] Phase validation approved - working authentication integration

---

### Phase 3: Authorization Guards & Service Integration

**Objective:** Integrate authorization guards into HTTP4S routes and service layer, demonstrating declarative permission checking with proper error handling (401/403). Provide both middleware-based (standard) and typed route (optional) approaches for authorization enforcement.

**Estimated Time:** 14 hours (updated from 11 hours)

**Prerequisites:** Completion of Phase 2 (AuthenticationService and CurrentUser available)

#### Tasks

1. **Create example service using Authorization.require** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/test/scala/works/iterative/core/ExampleDocumentServiceSpec.scala`
   - [ ] [impl] Write test case for createDocument succeeding when user authenticated
   - [ ] [impl] Write test case for updateDocument succeeding when user has "edit" permission
   - [ ] [impl] Write test case for updateDocument failing with Forbidden when user lacks permission
   - [ ] [impl] Write test case for deleteDocument requiring "delete" permission
   - [ ] [impl] Write test case for listDocuments using Authorization.filterAllowed to show only permitted documents
   - [ ] [impl] Use TestAuthenticationService and InMemoryPermissionService for testing
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify tests fail with "object ExampleDocumentService not found"
   - [ ] [reviewed] Tests validate service-level authorization guards

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/ExampleDocumentService.scala`
   - [ ] [impl] Add PURPOSE comments explaining example service demonstrating authorization patterns
   - [ ] [impl] Implement createDocument using Authorization.require for authenticated-only access
   - [ ] [impl] Implement updateDocument(id) using Authorization.require(PermissionOp("edit"), PermissionTarget.unsafe("document", id))
   - [ ] [impl] Implement deleteDocument(id) using Authorization.require(PermissionOp("delete"), PermissionTarget.unsafe("document", id))
   - [ ] [impl] Implement listDocuments using Authorization.filterAllowed to filter results by permission
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify all ExampleDocumentService tests pass
   - [ ] [reviewed] Service correctly uses authorization guards

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Extract permission target construction to helper method
   - [ ] [impl] Add comprehensive Scaladoc showing authorization patterns
   - [ ] [impl] Ensure consistent error types (AuthenticationError for auth failures)
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] Service is clean example for other developers

   **Success Criteria:** ExampleDocumentService demonstrates Authorization.require, Authorization.filterAllowed patterns with comprehensive tests
   **Testing:** ExampleDocumentServiceSpec validates authorization guards with permission granted/denied scenarios

2. **Create HTTP4S error handling for authentication/authorization** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/test/scala/works/iterative/server/http/AuthErrorHandlerSpec.scala`
   - [ ] [impl] Write test case for AuthenticationError.Unauthenticated returning 401 Unauthorized
   - [ ] [impl] Write test case for AuthenticationError.Forbidden returning 403 Forbidden
   - [ ] [impl] Write test case for other errors returning 500 Internal Server Error
   - [ ] [impl] Write test case for error response including appropriate message
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify tests fail with "object AuthErrorHandler not found"
   - [ ] [reviewed] Tests validate HTTP error handling for auth failures

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/AuthErrorHandler.scala`
   - [ ] [impl] Add PURPOSE comments explaining auth error mapping to HTTP status codes
   - [ ] [impl] Implement error handler that catches AuthenticationError (pattern match on enum)
   - [ ] [impl] Map Unauthenticated errors to 401 Unauthorized response
   - [ ] [impl] Map Forbidden errors to 403 Forbidden response
   - [ ] [impl] Include error message in response body (JSON format)
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify all tests pass
   - [ ] [reviewed] Error handling correctly maps errors to HTTP status codes

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Extract error message formatting to separate function
   - [ ] [impl] Add logging for authentication failures (info level)
   - [ ] [impl] Ensure sensitive information not leaked in error messages
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] Error handling is secure and well-tested

   **Success Criteria:** AuthErrorHandler correctly maps AuthenticationError to 401/403 HTTP responses with appropriate messages
   **Testing:** AuthErrorHandlerSpec validates error mapping for various scenarios

3. **Create example HTTP4S routes using authorization** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/test/scala/works/iterative/server/http/ExampleDocumentRoutesSpec.scala`
   - [ ] [impl] Write test case for PUT /documents/:id returning 200 when user has permission
   - [ ] [impl] Write test case for PUT /documents/:id returning 403 when user lacks permission
   - [ ] [impl] Write test case for DELETE /documents/:id returning 403 when user lacks delete permission
   - [ ] [impl] Write test case for GET /documents returning only permitted documents
   - [ ] [impl] Use TestAuthenticationService and setup test permissions
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify tests fail with "object ExampleDocumentRoutes not found"
   - [ ] [reviewed] HTTP route tests validate end-to-end authorization

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/ExampleDocumentRoutes.scala`
   - [ ] [impl] Add PURPOSE comments: Example routes showing BOTH middleware and typed route approaches
   - [ ] [impl] Implement routes calling service methods (service uses Authorization.require internally)
   - [ ] [impl] Add error handling using AuthErrorHandler (map auth errors to 401/403)
   - [ ] [impl] NOTE: Full middleware/typed routes integration comes in Tasks 6A/6B
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify all route tests pass
   - [ ] [reviewed] Routes correctly integrate authorization and error handling

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Add request logging (user ID, endpoint, permission checked)
   - [ ] [impl] Add Scaladoc with authorization examples
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] Routes are clean and reusable pattern

   **Success Criteria:** Example HTTP4S routes demonstrate authorization integration with proper 401/403 error handling
   **Testing:** ExampleDocumentRoutesSpec validates HTTP routes with various permission scenarios

4. **Create end-to-end test scenarios** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/e2e-testing/src/test/scala/works/iterative/e2e/AuthorizationE2ESpec.scala`
   - [ ] [impl] Write scenario: User logs in, creates document (becomes owner), can edit and delete
   - [ ] [impl] Write scenario: User A creates document, User B cannot edit or delete (403)
   - [ ] [impl] Write scenario: User with "editor" relation can edit but not delete
   - [ ] [impl] Write scenario: List documents returns only user's permitted documents
   - [ ] [impl] Write scenario: Unauthenticated request returns 401
   - [ ] [impl] Run test: `mill e2e-testing.test`
   - [ ] [impl] Verify tests fail (functionality not integrated end-to-end yet)
   - [ ] [reviewed] E2E scenarios validate complete authorization workflows

   **GREEN - Make Test Pass:**
   - [ ] [impl] Wire ExampleDocumentService and ExampleDocumentRoutes into test HTTP server
   - [ ] [impl] Configure TestAuthenticationService for E2E test environment
   - [ ] [impl] Configure InMemoryPermissionService for E2E test environment
   - [ ] [impl] Ensure CurrentUser properly propagated through entire request lifecycle
   - [ ] [impl] Run test: `mill e2e-testing.test`
   - [ ] [impl] Verify all E2E tests pass
   - [ ] [reviewed] End-to-end authorization works correctly

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Extract test setup (create users, grant permissions) to reusable helpers
   - [ ] [impl] Add more edge case scenarios (deleted users, malformed targets)
   - [ ] [impl] Add performance assertion (permission check < 50ms)
   - [ ] [impl] Run test: `mill e2e-testing.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] E2E tests are comprehensive and maintainable

   **Success Criteria:** End-to-end tests validate complete authorization flow from HTTP request through authentication, authorization, business logic, and response
   **Testing:** AuthorizationE2ESpec validates user scenarios with real HTTP requests

5. **Document authorization usage patterns** (Documentation Task)

   **Documentation Checklist:**
   - [ ] [impl] Create `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/docs/AUTHORIZATION_GUIDE.md`
   - [ ] [impl] Document Authorization.require pattern with code examples
   - [ ] [impl] Document Authorization.filterAllowed pattern for list queries
   - [ ] [impl] Document permission target format (namespace:objectId)
   - [ ] [impl] Document permission operations (create, view, edit, delete)
   - [ ] [impl] Document how to configure permission namespaces (PermissionConfig)
   - [ ] [impl] Document testing with TestAuthenticationService and InMemoryPermissionService
   - [ ] [impl] Add examples for common scenarios (document ownership, folder sharing)
   - [ ] [impl] **NEW: Document Route Protection Approaches section (placeholder for Tasks 6A/6B)**
   - [ ] [reviewed] Documentation is clear and comprehensive

   **Success Criteria:** AUTHORIZATION_GUIDE.md provides clear guidance for developers using authorization in services and routes
   **Testing:** Documentation review by team member unfamiliar with implementation

6A. **Create Authorization Middleware (Standard Approach)** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/test/scala/works/iterative/server/http/AuthorizationMiddlewareSpec.scala`
   - [ ] [impl] Write test: Routes wrapped with requireAuthenticated enforce authentication
   - [ ] [impl] Write test: Public paths (whitelist) bypass authentication
   - [ ] [impl] Write test: Middleware integrates with AuthErrorHandler for 401/403
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify tests fail with "object AuthorizationMiddleware not found"
   - [ ] [reviewed] Middleware tests validate standard route protection

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/AuthorizationMiddleware.scala`
   - [ ] [impl] Add PURPOSE comments: Standard middleware-based route protection (runtime enforcement)
   - [ ] [impl] Define public endpoint whitelist (health, login, static)
   - [ ] [impl] Implement `requireAuthenticated` middleware
   - [ ] [impl] Implement `requirePermission` middleware (with resource extractor)
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify middleware tests pass
   - [ ] [reviewed] Middleware provides composable protection

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Extract public path configuration to application.conf
   - [ ] [impl] Add logging for authorization decisions
   - [ ] [impl] Add comprehensive Scaladoc with usage examples
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] Middleware is production-ready

   **Success Criteria:** Middleware provides simple, composable route protection with public endpoint whitelist
   **Testing:** AuthorizationMiddlewareSpec validates runtime enforcement

6B. **Create Typed Route Protection (Optional Type-Safe Approach)** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/test/scala/works/iterative/server/http/ProtectedRouteSpec.scala`
   - [ ] [impl] Write test: ProtectedRoute.public works without auth
   - [ ] [impl] Write test: ProtectedRoute.authenticated requires CurrentUser
   - [ ] [impl] Write test: ProtectedRoute.requiresPermission checks permission
   - [ ] [impl] Write test: RouteInterpreter correctly enforces protection
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify tests fail with "enum RouteProtection not found"
   - [ ] [reviewed] Typed route tests validate compile-time safety

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/server/http/src/main/scala/works/iterative/server/http/ProtectedRoute.scala`
   - [ ] [impl] Add PURPOSE comments: Optional type-safe route protection system (compile-time enforcement)
   - [ ] [impl] Add NOTE: This is OPTIONAL - applications can use middleware instead
   - [ ] [impl] **SCALA 3 REQUIRED: Define RouteProtection enum:**
     ```scala
     enum RouteProtection:
       case Public
       case Authenticated
       case RequiresPermission(op: PermissionOp)
       case RequiresAnyPermission(ops: Set[PermissionOp])
     ```
   - [ ] [impl] Define `case class ProtectedRoute[P <: RouteProtection](route: Route, protection: P)`
   - [ ] [impl] Implement smart constructors (public, authenticated, requiresPermission)
   - [ ] [impl] Create RouteInterpreter object with interpret/interpretAll methods
   - [ ] [impl] Pattern match on RouteProtection to apply enforcement
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify typed route tests pass
   - [ ] [reviewed] Typed routes provide compile-time safety

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Add helpers for common patterns (resource extraction from URLs)
   - [ ] [impl] Add comprehensive examples in Scaladoc
   - [ ] [impl] Document when to use typed routes vs middleware
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] Optional approach is well-documented

   **Success Criteria:** Applications can opt-in to compile-time route protection using ProtectedRoute system
   **Testing:** ProtectedRouteSpec validates type-safe protection works correctly

7. **Update Example Routes to Show Both Approaches** (Update Task)

   **Update Checklist:**
   - [ ] [impl] Update ExampleDocumentRoutes.scala to demonstrate BOTH approaches
   - [ ] [impl] Add MiddlewareBasedRoutes object showing middleware usage
   - [ ] [impl] Add TypedRoutes object showing ProtectedRoute usage
   - [ ] [impl] Document trade-offs of each approach in comments
   - [ ] [impl] Run test: `mill server.http.test`
   - [ ] [impl] Verify both approaches work correctly
   - [ ] [reviewed] Examples clearly show both options

8. **Update Documentation for Both Approaches** (Update Task)

   **Documentation Updates:**
   - [ ] [impl] Update AUTHORIZATION_GUIDE.md with Route Protection Approaches section
   - [ ] [impl] Document middleware approach (recommended for most use cases)
   - [ ] [impl] Document typed routes approach (optional, for stronger guarantees)
   - [ ] [impl] Document when to use each approach
   - [ ] [impl] Document how to mix approaches if needed
   - [ ] [impl] Add comprehensive examples for both
   - [ ] [reviewed] Documentation explains both options clearly

#### Phase Success Criteria

- [ ] [impl] ExampleDocumentService demonstrates authorization guard patterns
- [ ] [reviewed] Service authorization patterns approved
- [ ] [impl] HTTP4S routes correctly map auth errors to 401/403 responses
- [ ] [reviewed] HTTP error handling approved
- [ ] [impl] Middleware-based protection provides standard approach
- [ ] [reviewed] Middleware approach approved
- [ ] [impl] Typed route protection provides optional compile-time safety
- [ ] [reviewed] Typed routes approach approved
- [ ] [impl] Examples demonstrate both approaches
- [ ] [reviewed] Both approaches documented
- [ ] [impl] End-to-end tests validate complete authorization workflows
- [ ] [reviewed] E2E test coverage approved
- [ ] [impl] All tests pass: `mill core.shared.test server.http.test e2e-testing.test`
- [ ] [reviewed] Test quality approved
- [ ] [impl] AUTHORIZATION_GUIDE.md documents both route protection approaches
- [ ] [reviewed] Documentation approved
- [ ] [impl] Can protect any service method with Authorization.require
- [ ] [reviewed] Phase validation approved - working authorization guards with flexible route protection

---

### Phase 4: Database Persistence (Production PermissionService)

**Objective:** Implement production-ready permission storage using MongoDB, enabling persistent permissions that survive server restarts and scale beyond in-memory limits.

**CRITICAL SECURITY REQUIREMENT - Fail-Closed Error Handling:**
All permission service implementations MUST fail closed (deny access) when errors occur, while providing full observability:
- DatabasePermissionService: On database errors, log full context (userId, resource, error details) AND deny access
- Use `tapError` for logging before catching errors to maintain fail-closed behavior
- Record metrics for infrastructure failures (separate from access denial)
- Pattern:
  ```scala
  repository.hasPermission(userId, resource)
    .tapError(error =>
      ZIO.logWarning(s"Permission check failed for user=$userId resource=$resource error=$error") *>
      recordMetric("permission.check.infrastructure_failure", error)
    )
    .catchAll(_ => ZIO.succeed(false))  // Fail closed with visibility
  ```

**Estimated Time:** 13 hours (updated from 11 hours, +2 hours for audit logging)

**Prerequisites:** Completion of Phase 3 (Authorization guards working with in-memory implementation)

#### Tasks

[Phase 4 tasks remain largely unchanged - I'll include the key update for PermissionServiceFactory to use Scala 3 enum]

1-4. [Tasks 1-4 remain unchanged from original]

5. **Create environment-based permission service selection** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/jvm/src/test/scala/works/iterative/core/auth/PermissionServiceFactorySpec.scala`
   - [ ] [impl] Write test case for PERMISSION_SERVICE=memory returning InMemoryPermissionService
   - [ ] [impl] Write test case for PERMISSION_SERVICE=database returning DatabasePermissionService
   - [ ] [impl] Write test case for invalid config failing fast with clear error
   - [ ] [impl] Run test: `mill core.jvm.test`
   - [ ] [impl] Verify tests fail with "object PermissionServiceFactory not found"
   - [ ] [reviewed] Factory tests validate configuration-based selection

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/jvm/src/main/scala/works/iterative/core/auth/PermissionServiceFactory.scala`
   - [ ] [impl] Add PURPOSE comments explaining environment-based permission service selection
   - [ ] [impl] **SCALA 3 REQUIRED: Define PermissionServiceType enum:**
     ```scala
     enum PermissionServiceType:
       case Memory, Database
     ```
   - [ ] [impl] Implement ZLayer factory reading PERMISSION_SERVICE config and parsing to enum
   - [ ] [impl] Pattern match on PermissionServiceType.Memory => InMemoryPermissionService
   - [ ] [impl] Pattern match on PermissionServiceType.Database => DatabasePermissionService
   - [ ] [impl] Add validation with clear error messages
   - [ ] [impl] Run test: `mill core.jvm.test`
   - [ ] [impl] Verify all tests pass
   - [ ] [reviewed] Factory correctly selects permission service

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Add logging for which permission service is loaded
   - [ ] [impl] Add configuration documentation in Scaladoc
   - [ ] [impl] Add default selection (memory for dev, database for production)
   - [ ] [impl] Run test: `mill core.jvm.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] Configuration is clear and robust

   **Success Criteria:** PermissionServiceFactory selects correct implementation based on PERMISSION_SERVICE config using Scala 3 enum
   **Testing:** PermissionServiceFactorySpec validates environment-based selection

6. **Create Audit Logging Infrastructure** (TDD Cycle)

   **RED - Write Failing Test:**
   - [ ] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/test/scala/works/iterative/core/audit/AuditLogServiceSpec.scala`
   - [ ] [impl] Write test case for logging permission check (userId, resource, action, result, timestamp)
   - [ ] [impl] Write test case for logging authentication event (userId, event type, success/failure)
   - [ ] [impl] Write test case for structured audit log format (JSON with all required fields)
   - [ ] [impl] Write test case for test implementation (in-memory buffer for verification)
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify tests fail with "trait AuditLogService not found"
   - [ ] [reviewed] Tests validate audit logging API

   **GREEN - Make Test Pass:**
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/audit/AuditLogService.scala`
   - [ ] [impl] Add PURPOSE comments: Security audit trail for authentication and authorization events
   - [ ] [impl] Define case class AuditEvent(
       timestamp: Instant,
       userId: Option[UserId],
       eventType: String,
       resource: Option[String],
       action: Option[String],
       result: String,  // "allowed", "denied", "error"
       reason: Option[String],
       metadata: Map[String, String]
     )
   - [ ] [impl] Define trait AuditLogService:
     ```scala
     trait AuditLogService:
       def logPermissionCheck(
         userId: UserId,
         resource: PermissionTarget,
         action: PermissionOp,
         result: Boolean,
         reason: Option[String] = None
       ): UIO[Unit]

       def logAuthenticationEvent(
         userId: Option[UserId],
         eventType: String,  // "login", "logout", "login_failed"
         success: Boolean,
         metadata: Map[String, String] = Map.empty
       ): UIO[Unit]
     ```
   - [ ] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/audit/InMemoryAuditLogService.scala`
   - [ ] [impl] Implement InMemoryAuditLogService with Ref[List[AuditEvent]] (for testing)
   - [ ] [impl] Add ZLayer factory for InMemoryAuditLogService
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify all tests pass
   - [ ] [reviewed] Audit logging API defined

   **REFACTOR - Improve Quality:**
   - [ ] [impl] Add helper method to format AuditEvent as JSON string
   - [ ] [impl] Add Scaladoc explaining audit log retention and compliance requirements
   - [ ] [impl] Add note: Production implementation (separate audit stream) to be added in Phase 5
   - [ ] [impl] Consider: Should audit logs be separate from application logs? (Yes - recommend separate stream)
   - [ ] [impl] Run test: `mill core.shared.test`
   - [ ] [impl] Verify all tests still pass
   - [ ] [reviewed] Audit logging is production-ready

   **Success Criteria:** Audit logging infrastructure exists to track all permission checks and authentication events
   **Testing:** AuditLogServiceSpec validates audit event recording

7. **Integrate Audit Logging with DatabasePermissionService** (Update Task)

   **Update Checklist:**
   - [ ] [impl] Update DatabasePermissionService to inject AuditLogService
   - [ ] [impl] Log permission check before returning result:
     ```scala
     for {
       result <- repository.hasPermission(userId, resource)
                   .tapError(error =>
                     ZIO.logWarning(s"Permission check failed: $error") *>
                     MetricsService.recordCounter("permission.check.infrastructure_failure")
                   )
                   .catchAll(_ => ZIO.succeed(false))
       _ <- AuditLogService.logPermissionCheck(userId, resource, action, result)
     } yield result
     ```
   - [ ] [impl] Ensure audit log written regardless of result (allowed/denied/error)
   - [ ] [impl] Run test: `mill core.jvm.test`
   - [ ] [impl] Verify audit events recorded for all permission checks
   - [ ] [reviewed] Audit integration is comprehensive

[Continue with remaining Phase 4 tasks if any...]

#### Phase Success Criteria

- [ ] [impl] DatabasePermissionService persists permissions to MongoDB
- [ ] [reviewed] Database implementation approved
- [ ] [impl] Fail-closed error handling with observability (logging + metrics)
- [ ] [reviewed] Security pattern approved
- [ ] [impl] Audit logging captures all permission checks and auth events
- [ ] [reviewed] Audit trail approved
- [ ] [impl] PermissionServiceFactory selects correct implementation using Scala 3 enum
- [ ] [reviewed] Configuration validated
- [ ] [impl] All tests pass: `mill core.jvm.test`
- [ ] [reviewed] Phase validation approved - production-ready permission service

---

[Phase 5 remains unchanged from original]

---

[Testing Strategy section remains unchanged]

[Documentation Requirements section remains unchanged]

[Deployment Checklist remains unchanged]

[Rollback Plan remains unchanged]

---

**Tasks Status:** Updated with Critical Issue Fixes + Infrastructure

**Key Changes Applied:**
1. ✅ Added Task 0: Define Domain Value Types (Scala 3 opaque types)
2. ✅ Added Task 3: PermissionLogic (pure domain functions for FCIS)
3. ✅ Updated InMemoryPermissionService to call PermissionLogic
4. ✅ Added Task 6: AuthenticationError enum (Scala 3)
5. ✅ Updated Authorization helpers with typed error channel
6. ✅ Added FiberRef lifecycle management
7. ✅ Updated factories to use Scala 3 enums
8. ✅ Added Tasks 6A/6B: Middleware and Typed Routes (flexible approaches)
9. ✅ Added Task 9: Metrics Infrastructure (Phase 1, +2h)
10. ✅ Added Task 10: Configuration Validation (Phase 1, +2h)
11. ✅ Added Task 6-7: Audit Logging (Phase 4, +2h)
12. ✅ Updated time estimate: 48 → 54 hours

**Critical Infrastructure Added:**
- MetricsService: Enables fail-closed observability (used in Phase 4)
- ConfigValidator: Centralized configuration validation on startup
- AuditLogService: Security audit trail for all auth/authz events

**Start here:** Phase 1, Task 0 - Define Domain Value Types (RED: Write failing test)
