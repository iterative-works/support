# Plan Review Summary - IWSD-74

**Date:** 2025-11-15
**Review Type:** Implementation Plan Review (tasks.md + analysis.md)
**Status:** All Critical Issues Resolved

## Executive Summary

The implementation plan for IWSD-74 (Authentication & Authorization System) underwent comprehensive review using the plan-reviewer skill. The review identified **7 critical architectural issues** that required immediate attention before implementation. All critical issues have been resolved with pragmatic, FCIS-aligned solutions.

**Key Outcomes:**
- ✅ All 7 critical issues addressed
- ✅ FCIS (Functional Core, Imperative Shell) architecture enforced
- ✅ Scala 3 idioms made mandatory (opaque types, enums)
- ✅ Fail-closed security with observability implemented
- ✅ Dual authorization approach (middleware + typed routes)
- ✅ Strict input validation for PermissionTarget
- ✅ Updated time estimate: 44 → 48 hours

## Critical Issues & Resolutions

### ❌ Issue #1: PermissionService Violates FCIS
**Problem:** PermissionService interface had ZIO effects in what should be pure domain logic.

**Resolution: Pragmatic FCIS Split**
- Created `PermissionLogic` object with **pure functions** (functional core):
  ```scala
  def isAllowed(userId: UserId, action: PermissionOp, target: PermissionTarget,
                tuples: Set[RelationTuple], config: PermissionConfig): Boolean
  def listAllowed(userId: UserId, action: PermissionOp, namespace: String,
                  tuples: Set[RelationTuple], config: PermissionConfig): Set[String]
  ```
- `PermissionService` implementations call pure logic (imperative shell):
  ```scala
  def isAllowed(userId: UserId, action: PermissionOp, target: PermissionTarget): UIO[Boolean] =
    storage.get.map { tuples =>
      PermissionLogic.isAllowed(userId, action, target, tuples, config)
    }
  ```
- Kept existing interface locations (application layer, not domain)
- Added Task 3: Create PermissionLogic (pure domain functions)

**Files Changed:**
- `tasks.md`: Added Task 3, updated Task 4 (InMemoryPermissionService)
- `analysis.md`: Added PermissionLogic to Components, updated FCIS boundaries

---

### ❌ Issue #2: Authorization Helpers Mix Domain/Infrastructure
**Problem:** Authorization helpers (require, withPermission) combine effects with business logic.

**Resolution: Recognize as Application Layer**
- Acknowledged helpers coordinate `PermissionService` + `CurrentUser` (application layer)
- Not pure domain - they orchestrate infrastructure concerns
- Updated documentation to clarify architectural layer

**Files Changed:**
- `tasks.md`: No structural changes, clarified purpose in comments
- `analysis.md`: Moved Authorization.scala to Application Layer section

---

### ❌ Issue #3: InMemoryPermissionService Misplaced in Domain
**Problem:** In-memory implementation with `Ref[Set[RelationTuple]]` belongs in infrastructure.

**Resolution: Resolved by Issue #1 Approach**
- InMemoryPermissionService now calls pure `PermissionLogic` functions
- Recognized as application-layer infrastructure (effects + storage)
- Pure logic extracted to PermissionLogic (domain)

**Files Changed:**
- `tasks.md`: Updated Task 4 to call PermissionLogic functions
- `analysis.md`: Moved InMemoryPermissionService to Application Layer

---

### ❌ Issue #4: Scala 3 Idioms Not Explicitly Required
**Problem:** Plan said "could be", "likely" instead of mandating Scala 3 features.

**Resolution: Mandatory Scala 3 Features**
- **Opaque types REQUIRED:** UserId, PermissionOp, PermissionTarget
- **Enums REQUIRED:** AuthProvider, PermissionServiceType, AuthenticationError, RouteProtection
- Added "**SCALA 3 REQUIRED**" markers throughout tasks
- Added Task 0: Define Domain Value Types (opaque types with validation)
- Added Task 6: Define AuthenticationError enum

**Files Changed:**
- `tasks.md`: Task 0 (opaque types), Task 6 (error enum), factory enums in Phases 2-4
- `analysis.md`: Updated Technology Choices, all component descriptions

---

### ❌ Issue #5: Missing Authorization Middleware Integration
**Problem:** No clear strategy for ensuring routes are protected.

**Resolution: Dual Approach (Middleware + Typed Routes)**
- **Option A: Authorization Middleware (Standard)**
  - Runtime enforcement, flexible error handling
  - Recommended for most use cases
  - Task 6A in Phase 3

- **Option B: Typed Route Protection (Optional)**
  - Compile-time safety using RouteProtection enum
  - Type-safe guarantee: `ProtectedRoute[RequiresPermission]`
  - Prevents accidental unprotected routes
  - Task 6B in Phase 3

**Files Changed:**
- `tasks.md`: Added Tasks 6A and 6B, updated examples (Task 7), documentation (Task 8)
- `analysis.md`: Added "Authorization Enforcement (Dual Approach)" to Presentation Layer

---

### ❌ Issue #6: Fail-Closed Error Handling Swallows Information
**Problem:** Permission check errors denied access silently, losing debugging context.

**Resolution: Fail-Closed with Observability**
- **Security:** Still fail closed (deny access on errors)
- **Observability:** Log full context before denying
- **Pattern:**
  ```scala
  repository.hasPermission(userId, resource)
    .tapError(error =>
      ZIO.logWarning(s"Permission check failed for user=$userId resource=$resource error=$error") *>
      recordMetric("permission.check.infrastructure_failure", error)
    )
    .catchAll(_ => ZIO.succeed(false))  // Fail closed with visibility
  ```
- Structured logging captures: userId, resource, operation, error details
- Metrics track infrastructure failures separately from access denials

**Files Changed:**
- `tasks.md`: Added "CRITICAL SECURITY REQUIREMENT" to Phase 4 with code pattern
- `analysis.md`: Updated Risk #5 mitigation with fail-closed observability

---

### ❌ Issue #7: PermissionTarget Validation Not Specified
**Problem:** No validation requirements for PermissionTarget format.

**Resolution: Strict Input Validation**
- **Format:** `namespace:objectId`
- **Namespace:** `[a-z][a-z0-9_]*` (lowercase, max 50 chars)
- **ObjectId:** `[a-zA-Z0-9-]+`
- **Safe parsing:** `parse(value: String): Either[ValidationError, PermissionTarget]`
- **Unsafe constructor:** `unsafe(namespace, objectId)` for pre-validated inputs
- **Rejections:** uppercase namespace, special chars, missing colon, empty parts, length violations

**Comprehensive test requirements:**
- Valid: "document:123", "task_list:abc-123"
- Invalid: missing colon, uppercase, special chars, length violations, empty parts

**Files Changed:**
- `tasks.md`: Enhanced Task 0 with 8+ validation test cases
- `analysis.md`: Added strict validation to Components, new Input Validation test section

---

## Implementation Plan Changes

### tasks.md Updates

**New Tasks Added:**
- **Task 0:** Define Domain Value Types (Scala 3 opaque types) - 2 hours
- **Task 3:** Create PermissionLogic (pure domain functions) - 3 hours
- **Task 6:** Define AuthenticationError enum - 1 hour
- **Task 6A:** Authorization Middleware - 2 hours
- **Task 6B:** Typed Route Protection - 3 hours

**Tasks Modified:**
- **Task 4:** InMemoryPermissionService now calls PermissionLogic
- **Phase 2 Task 2:** Added FiberRef lifecycle management (ZIO.scoped)
- **Phase 2 Task 4:** AuthenticationServiceFactory uses AuthProvider enum
- **Phase 4 Task 5:** PermissionServiceFactory uses PermissionServiceType enum

**Time Estimate Updated:** 44 → 48 hours (+4 hours for new tasks)

### analysis.md Updates

**Architecture Section:**
- Added FCIS architecture note in header
- Updated Patterns to Use with explicit FCIS description
- Enhanced Functional Boundaries section

**Technology Choices:**
- Made Scala 3 features MANDATORY (opaque types, enums)

**Components Section:**
- Added ValueTypes.scala (Domain Layer) with strict validation
- Added PermissionLogic.scala (Domain Layer - functional core)
- Added AuthenticationError.scala (Domain Layer)
- Moved InMemoryPermissionService to Application Layer
- Moved Authorization helpers to Application Layer

**Presentation Layer:**
- Added dual authorization approach (middleware vs typed routes)

**Testing Strategy:**
- Added PermissionLogicSpec (pure function testing)
- Added DomainTypesSpec with comprehensive validation tests
- Added FiberRef lifecycle testing requirements

**Risk Mitigation:**
- Enhanced Risk #5 with fail-closed observability pattern

---

## Architectural Decisions

### 1. FCIS (Functional Core, Imperative Shell) Enforcement

**Decision:** Extract pure permission logic to PermissionLogic object.

**Rationale:**
- Separates "what to check" (pure) from "how to check" (effects)
- Pure functions are trivially testable (no mocking)
- Service interfaces orchestrate effects and call pure logic
- Maintains pragmatic approach (no file moves required)

**Impact:**
- Better testability (PermissionLogic has zero dependencies)
- Clearer separation of concerns
- Easier to reason about permission rules

---

### 2. Mandatory Scala 3 Idioms

**Decision:** Require opaque types and enums throughout.

**Rationale:**
- Opaque types provide zero-cost type safety (UserId ≠ PermissionOp)
- Enums are cleaner than sealed trait + case objects
- Prevents accidental type confusion
- Modern Scala 3 best practices

**Impact:**
- Compile-time prevention of type errors
- Better IDE support and autocomplete
- Clearer domain modeling

---

### 3. Dual Authorization Approach

**Decision:** Provide both middleware (standard) and typed routes (optional).

**Rationale:**
- Middleware: Flexible, works with existing code, runtime checks
- Typed routes: Compile-time safety, prevents forgetting protection
- Different projects have different needs
- No forcing single approach

**Impact:**
- Teams can choose based on requirements
- Typed routes prevent entire class of security bugs
- Middleware provides easier migration path

---

### 4. Fail-Closed with Observability

**Decision:** Deny access on errors while logging full context.

**Rationale:**
- Security: Never accidentally allow access due to bugs
- Operations: Need visibility into failures for debugging
- Metrics: Track infrastructure health separately from access denials
- Use `tapError` pattern to log before catching

**Impact:**
- Maintains security posture (fail closed)
- Enables debugging of permission system issues
- Separates security events from infrastructure problems

---

### 5. Strict PermissionTarget Validation

**Decision:** Validate format at construction time with comprehensive tests.

**Rationale:**
- Invalid targets can bypass permission checks
- Prevents injection attacks via malformed targets
- Enforces consistent naming conventions
- Early failure at API boundary

**Impact:**
- Security: Invalid input rejected before reaching permission logic
- Reliability: Consistent format throughout system
- Developer experience: Clear error messages on validation failures

---

## Next Steps

### Before Implementation

1. ✅ Plan review completed
2. ✅ All critical issues resolved
3. ✅ tasks.md updated with fixes
4. ✅ analysis.md updated with fixes
5. ⏳ Update Linear issue with review results (this document)
6. ⏳ Get stakeholder approval on architectural decisions
7. ⏳ Begin Phase 1 implementation (Task 0: Define Domain Value Types)

### Implementation Order

Following TDD approach (RED-GREEN-REFACTOR):

**Phase 1: Permission Foundation** (10 hours)
- Start: Task 0 (Domain Value Types with opaque types)
- Continue: Tasks 1-8 as specified in tasks.md

**Phase 2: Authentication Integration** (10 hours)
- Implement Pac4J adapter and TestAuthenticationService
- Add FiberRef lifecycle management
- Use AuthProvider enum for factory

**Phase 3: Authorization Guards** (14 hours)
- Create authorization helpers
- Implement BOTH middleware and typed routes
- Comprehensive E2E tests

**Phase 4: Database Persistence** (11 hours)
- Implement fail-closed pattern with observability
- Use PermissionServiceType enum for factory

**Phase 5: Optimization** (3 hours)
- Production readiness and performance tuning

---

## Warnings/Suggestions (Lower Priority)

The plan review may have identified additional warnings and suggestions beyond the 7 critical issues. These can be addressed during implementation or in a follow-up iteration:

- Code organization refinements
- Performance optimizations
- Additional test coverage
- Documentation improvements
- Monitoring and alerting setup

---

## Conclusion

All critical architectural issues have been resolved with pragmatic solutions that:
- ✅ Enforce FCIS principles (functional core + imperative shell)
- ✅ Mandate Scala 3 idioms for type safety
- ✅ Provide flexible authorization enforcement
- ✅ Maintain fail-closed security with observability
- ✅ Validate all inputs strictly

The implementation plan is now ready for execution. The updated time estimate of **48 hours** reflects the additional tasks for opaque types, pure logic extraction, enums, and dual authorization approaches.

**Recommendation:** Proceed with Phase 1 implementation starting with Task 0 (Define Domain Value Types).
