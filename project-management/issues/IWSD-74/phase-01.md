# Phase 1: Permission Foundation (In-Memory Implementation)

**Issue:** IWSD-74
**Phase:** 1 of 4
**Objective:** Create working permission system with in-memory storage, enabling immediate testing of Zanzibar-inspired ReBAC model without database dependencies. Also establish metrics and configuration infrastructure.
**Estimated Time:** 14 hours (updated from 10 hours, +4 hours for metrics and config validation)
**Prerequisites:** None (foundational phase)

## Phase Objective

Establish the foundational permission system using in-memory storage, implementing a Zanzibar-inspired ReBAC (Relationship-Based Access Control) model. This phase delivers a working authorization system that can be tested immediately without database dependencies. The implementation follows FCIS (Functional Core, Imperative Shell) principles, with pure domain logic separated from effect-based orchestration.

Key deliverables:
- Domain value types using Scala 3 opaque types (compile-time safety, zero runtime cost)
- Pure permission checking logic (PermissionLogic) testable without ZIO
- In-memory permission service for immediate use
- Authorization helpers for declarative permission guards
- Metrics and configuration infrastructure for observability

## Tasks

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

## Phase Success Criteria

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

**Phase Status:** Completed
**Next Phase:** Phase 2: Authentication Integration - see [phase-02.md](../phase-02.md)
