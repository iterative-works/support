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
   - [x] [impl] Create test file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/test/scala/works/iterative/core/auth/DomainTypesSpec.scala`
   - [x] [impl] Write test case for UserId creation and value extraction
   - [x] [impl] Write test case for PermissionOp creation
   - [x] [impl] **CRITICAL: Write comprehensive PermissionTarget validation tests:**
     - Valid format: "document:123" → Right(PermissionTarget)
     - Valid with underscore: "task_list:abc-123" → Right(PermissionTarget)
     - Invalid: missing colon → Left(ValidationError)
     - Invalid: uppercase namespace → Left(ValidationError)
     - Invalid: special chars in namespace → Left(ValidationError)
     - Invalid: namespace > 50 chars → Left(ValidationError)
     - Invalid: empty namespace → Left(ValidationError)
     - Invalid: empty objectId → Left(ValidationError)
   - [x] [impl] Write test case for PermissionTarget.unsafe constructor (no validation)
   - [x] [impl] Write test case for namespace and objectId extraction
   - [x] [impl] Write test case that UserId cannot be assigned to PermissionOp (type safety)
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify tests fail with "type UserId not found"
   - [x] [reviewed] Tests validate type-safe domain identifiers with strict input validation

   **GREEN - Make Test Pass:**
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/DomainTypes.scala`
   - [x] [impl] Add PURPOSE comments: Type-safe domain identifiers using Scala 3 opaque types
   - [x] [impl] **SCALA 3 REQUIRED: Define UserId as opaque type:**
     ```scala
     opaque type UserId = String
     object UserId:
       def apply(value: String): UserId = value
       extension (id: UserId)
         def value: String = id
     ```
   - [x] [impl] **SCALA 3 REQUIRED: Define PermissionOp as opaque type:**
     ```scala
     opaque type PermissionOp = String
     object PermissionOp:
       def apply(value: String): PermissionOp = value
       extension (op: PermissionOp)
         def value: String = op
     ```
   - [x] [impl] **SCALA 3 REQUIRED: Define PermissionTarget as opaque type with validation:**
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
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all type tests pass
   - [x] [reviewed] Opaque types provide zero-cost compile-time safety

   **REFACTOR - Improve Quality:**
   - [x] [impl] Add comprehensive Scaladoc explaining opaque types benefits
   - [x] [impl] Add usage examples in documentation
   - [x] [impl] Ensure validation errors are descriptive
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all tests still pass
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
   - [x] [impl] Create file: `/home/mph/.local/share/par/worktrees/d105e143/IWSD-74/core/shared/src/main/scala/works/iterative/core/auth/InMemoryPermissionService.scala`
   - [x] [impl] Add PURPOSE comments explaining in-memory ReBAC permission service for testing/simple deployments
   - [x] [impl] Add NOTE: This is application-layer infrastructure (effects + storage), not domain
   - [x] [impl] Implement class with `Ref[Set[RelationTuple]]` storage
   - [x] [impl] **KEY: Call PermissionLogic.isAllowed (pure function) from implementation:**
     ```scala
     def isAllowed(userId: UserId, action: PermissionOp, target: PermissionTarget): UIO[Boolean] =
       storage.get.map { tuples =>
         PermissionLogic.isAllowed(userId, action, target, tuples, config)
       }
     ```
   - [x] [impl] Implement addRelation, removeRelation using Ref.update
   - [x] [impl] Implement listAllowed calling PermissionLogic.listAllowed
   - [x] [impl] Create ZLayer factory method with ZLayer.fromZIO pattern (lazy initialization)
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all InMemoryPermissionService tests pass
   - [x] [reviewed] Implementation correctly delegates to pure logic

   **REFACTOR - Improve Quality:**
   - [x] [impl] Add error handling (fail closed on unexpected cases)
   - [x] [impl] Optimize listAllowed query (single Ref.get, filter in memory)
   - [x] [impl] Add comprehensive Scaladoc with usage examples
   - [x] [impl] Run test: `mill core.shared.test`
   - [x] [impl] Verify all tests still pass
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

- [x] [impl] Opaque types provide compile-time safety for all domain IDs
- [x] [reviewed] Scala 3 type definitions approved
- [x] [impl] PermissionLogic provides pure, testable permission checking functions
- [x] [reviewed] FCIS separation approved (pure logic vs effects)
- [x] [impl] RelationTuple case class correctly represents (user, relation, target) triples
- [x] [reviewed] RelationTuple design approved
- [x] [impl] PermissionConfig defines namespace-specific permission inheritance rules with depth limits
- [x] [reviewed] PermissionConfig design approved
- [x] [impl] InMemoryPermissionService checks permissions by delegating to pure PermissionLogic
- [x] [reviewed] InMemoryPermissionService implementation approved
- [x] [impl] Authorization helpers provide declarative guards (require, check, withPermission, filterAllowed)
- [x] [reviewed] Authorization helper API approved
- [x] [impl] MetricsService abstraction exists with no-op implementation
- [x] [reviewed] Metrics infrastructure approved
- [x] [impl] ConfigValidator validates all configuration on startup
- [x] [reviewed] Configuration validation approved
- [x] [impl] All unit tests pass: `mill core.shared.test`
- [x] [reviewed] Test coverage and quality approved (100% coverage of permission logic)
- [x] [impl] Can grant user "owner" on document:123, verify they have "view" via inheritance
- [x] [reviewed] Phase validation approved - working in-memory permission system with infrastructure

---

### Phase 2: Authentication Integration (Pac4J & Test Mode)

**Objective:** Bridge Pac4J OIDC integration with AuthenticationService interface and create test authentication mode for rapid development without OIDC dependencies.

**Estimated Time:** 10 hours (updated from 9 hours)

**Prerequisites:** Completion of Phase 1 (Authorization helpers exist for integration testing)

#### Tasks

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

#### Phase Success Criteria

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

### Phase 3: Authorization Guards & Service Integration

**Objective:** Integrate authorization guards into HTTP4S routes and service layer, demonstrating declarative permission checking with proper error handling (401/403). Provide both middleware-based (standard) and typed route (optional) approaches for authorization enforcement.

**Estimated Time:** 14 hours (updated from 11 hours)

**Prerequisites:** Completion of Phase 2 (AuthenticationService and CurrentUser available)

#### Tasks

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

#### Phase Success Criteria

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
