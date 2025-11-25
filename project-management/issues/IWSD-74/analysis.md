# Technical Analysis: Implement Authentication & Authorization System

**Issue:** IWSD-74
**Created:** 2025-11-13
**Updated:** 2025-11-15
**Status:** Draft
**Classification:** Feature

> **ARCHITECTURE NOTE:** This implementation follows **Functional Core, Imperative Shell (FCIS)** principles. Pure domain logic (permission checking, relation resolution) is extracted into `PermissionLogic` as pure functions. Service interfaces (`PermissionService`, `AuthenticationService`) coordinate effects and orchestration in the application/infrastructure layers.

## Problem Statement

Our HTTP4S server application needs a production-ready authentication and authorization system that meets four specific requirements:

1. **Pluggable authentication mechanism** - We need to support OIDC for production deployments and a simple account picker for testing/development environments
2. **Universal user information model** - A standard way to work with user data (persistent user ID, user name, email, roles) that can be extended with project-specific attributes
3. **Project-specific extensibility** - Support for additional user information like organizational position, titles, or other application-specific data
4. **Declarative authorization** - Ability to obtain authenticated user information anywhere in our ZIO application and protect code declaratively using higher-order effects

**Current State:** We have excellent foundations already in place (Zanzibar-inspired PermissionService interface, complete user model hierarchy, AuthenticationService with FiberRef implementation, CurrentUser ZIO service), but they lack implementations and integration. The comprehensive research (research.md) has been completed, identifying the existing components and proposing an implementation strategy.

**Impact:** Without a complete authentication/authorization system, we cannot:
- Securely deploy applications to production
- Test authentication flows easily in development
- Implement fine-grained access control
- Audit who has access to what resources
- Support complex permission scenarios (ownership, delegation, inheritance)

## Proposed Solution

### High-Level Approach

Rather than designing new systems, we will **implement and integrate the existing, well-designed components** already present in the codebase. The solution consists of five major workstreams:

1. **Implement PermissionService** - Create InMemoryPermissionService (for testing/simple deployments) and DatabasePermissionService (for production) implementations of the existing PermissionService interface, using a Zanzibar-inspired relationship-based access control (ReBAC) model

2. **Integrate Pac4J with AuthenticationService** - Create Pac4jAuthenticationAdapter that bridges the existing Pac4J integration with our AuthenticationService interface, mapping Pac4J CommonProfile to BasicProfile

3. **Build TestAuthenticationService** - Implement a test authentication provider that allows easy user switching without requiring real OIDC, enabling rapid development and testing

4. **Create Authorization helpers** - Develop declarative authorization guards (require, withPermission, check, filterAllowed) that integrate PermissionService with CurrentUser for business logic

5. **Optimize query patterns** - Implement efficient authorization-aware repository queries using two-phase lookup (reverse permission queries followed by filtered data queries)

This approach builds on existing abstractions, requires no breaking changes, and can be adopted incrementally service-by-service.

### Why This Approach

**Leverages Existing Investment:**
- PermissionService interface is already well-designed with Zanzibar-inspired model
- UserInfo/UserProfile/BasicProfile hierarchy is complete and functional
- CurrentUser ZIO service already provides fiber-local user context
- AuthenticationService interface exists with FiberRef implementation
- Pac4J integration is already present in server/http module

**Zanzibar Model Superiority:**
- More flexible than role-based access control (RBAC)
- Supports fine-grained permissions (per-resource, not just per-role)
- Enables relationship-based authorization (ownership, group membership, delegation)
- Allows permission inheritance (owner implies editor implies viewer)
- Centralizes permission logic instead of scattering role checks throughout code
- Enables efficient queries for "what can user X access?" (reverse lookup)

**Risk Mitigation:**
- No need to replace existing code - we extend and implement
- Can be adopted incrementally (new features first, migrate existing features gradually)
- Test implementations enable development without production dependencies
- In-memory implementation provides immediate value without database setup

**Alternative Considered and Rejected:**
- **Pure role-based authorization** - Too inflexible, can't express "alice owns document:123"
- **External authorization services (SpiceDB, OpenFGA)** - Adds operational complexity, network latency; overkill for initial implementation
- **Replace Pac4J entirely** - High risk, Pac4J works well for OIDC; better to abstract behind interface

## Technical Decisions

### Architecture

**Layers Affected:**
- **Domain Layer** - PermissionService implementations, Authorization helpers, RelationTuple model
- **Application Layer** - Integration of authentication and authorization in service methods
- **Infrastructure Layer** - PermissionRepository for database persistence, Pac4jAuthenticationAdapter
- **Presentation Layer** - HTTP4S middleware integration to extract user context from requests

**Patterns to Use:**
- **Repository Pattern** - PermissionRepository for storing relation tuples
- **Adapter Pattern** - Pac4jAuthenticationAdapter to bridge Java-based Pac4J to Scala/ZIO types
- **Strategy Pattern** - Pluggable AuthenticationService and PermissionService implementations
- **Service Layer** - Authorization helpers coordinate PermissionService and CurrentUser
- **Functional Core, Imperative Shell (FCIS)** - `PermissionLogic` object contains pure permission checking functions; service interfaces (`PermissionService`, `AuthenticationService`) handle effects and orchestration

**Integration Points:**
- AuthenticationService.provideCurrentUser → CurrentUser ZIO environment layer
- PermissionService.isAllowed + CurrentUser → Authorization guards
- HTTP4S request → AuthenticationService.loggedIn → FiberRef storage
- Pac4J CommonProfile → BasicProfile mapping
- PermissionRepository ↔ Database (MongoDB or similar)
- Service layer methods → Authorization.require/withPermission wrappers

### Technology Choices

**Frameworks/Libraries:**
- **ZIO 2.x** - Effect system, FiberRef for user context, ZLayer for dependency injection
- **HTTP4S** - HTTP server framework (already in use)
- **Pac4J** - OIDC authentication (already in use, keep for production)
- **Scala 3** - **MANDATORY** use of opaque types (UserId, PermissionOp, PermissionTarget), enums (AuthProvider, PermissionServiceType, AuthenticationError, RouteProtection), extension methods
- **MongoDB** - Database storage for production PermissionService (inferred from repository files)

**Data Storage:**
- **FiberRef** - Store current user in fiber-local storage (already implemented in FiberRefAuthentication)
- **In-memory (Ref[Set[RelationTuple]])** - For test/development PermissionService
- **Database collection** - For production PermissionService relation tuples with compound indexes

**External Systems:**
- **OIDC Provider** - For production authentication (via Pac4J)
- **MongoDB** - For persistent permission storage
- **No external authorization service** - Self-contained implementation initially

### Design Patterns

**Zanzibar-Inspired ReBAC:**
- Relation tuples: (user, relation, target) representing "user has relation to target"
- Permission checking with computed relations (owner implies viewer)
- Namespace configuration for permission inheritance rules
- Efficient reverse lookup: "what can user X access?"

**DDD Concepts:**
- **Value Objects**: UserId, PermissionOp, PermissionTarget (Scala 3 opaque types), UserRole
- **Entities**: RelationTuple, BasicProfile
- **Domain Services**: PermissionService, AuthenticationService (application layer, orchestrate effects)
- **Pure Domain Logic**: PermissionLogic object (functional core, pure functions)
- **Repository**: PermissionRepository for relation tuple persistence
- **Specifications**: PermissionConfig defines rules for each namespace

**Functional Boundaries (FCIS):**
- **Functional Core**: `PermissionLogic` object with pure functions (isAllowed, listAllowed, resolveComputedRelations)
- **Imperative Shell**: Service interfaces handle effects (database queries, FiberRef access, HTTP request handling)
- **Domain to Application**: `InMemoryPermissionService` and `DatabasePermissionService` call pure `PermissionLogic` functions
- **Immutable data**: RelationTuple, BasicProfile, PermissionConfig are all immutable case classes

## Components to Modify/Create

### Domain Layer

**Create:**
- `core/shared/src/main/scala/works/iterative/core/auth/ValueTypes.scala`
  - **SCALA 3 REQUIRED**: Opaque types for UserId, PermissionOp, PermissionTarget
  - **CRITICAL: PermissionTarget strict validation:**
    - Format: `namespace:objectId` where namespace = `[a-z][a-z0-9_]*` (max 50 chars)
    - objectId = `[a-zA-Z0-9-]+`
    - `parse(value: String): Either[ValidationError, PermissionTarget]` for safe parsing
    - `unsafe(namespace, objectId)` for pre-validated inputs
    - Reject: uppercase namespace, special chars, missing colon, empty parts, length violations
  - Extension methods for safe access to underlying values
- `core/shared/src/main/scala/works/iterative/core/auth/RelationTuple.scala`
  - Case class: `RelationTuple(user: UserId, relation: String, target: PermissionTarget)`
- `core/shared/src/main/scala/works/iterative/core/auth/PermissionConfig.scala`
  - Case classes: `PermissionConfig`, `NamespaceConfig`
  - Permission inheritance rules (owner → editor → viewer)
- `core/shared/src/main/scala/works/iterative/core/auth/PermissionLogic.scala`
  - **FUNCTIONAL CORE**: Pure functions for permission checking
  - `isAllowed`, `listAllowed`, `resolveComputedRelations` (all pure)
  - No ZIO effects, testable with simple inputs/outputs
- `core/shared/src/main/scala/works/iterative/core/auth/AuthenticationError.scala`
  - **SCALA 3 REQUIRED**: Enum for authentication errors (Unauthenticated, Forbidden, etc.)

### Application Layer

**Create:**
- `core/shared/src/main/scala/works/iterative/core/auth/InMemoryPermissionService.scala`
  - **IMPERATIVE SHELL**: Orchestrates effects (Ref[Set[RelationTuple]])
  - Calls pure `PermissionLogic` functions for actual checking
- `core/shared/src/main/scala/works/iterative/core/auth/Authorization.scala`
  - **APPLICATION LAYER**: Helper methods require, withPermission, check, filterAllowed
  - Coordinates PermissionService + CurrentUser (not pure domain)
- `core/shared/src/main/scala/works/iterative/core/auth/AlwaysAllowPermissionService.scala`
  - Simple test implementation that always returns true

**Create (Infrastructure Services):**
- `core/shared/src/main/scala/works/iterative/core/metrics/MetricsService.scala`
  - Abstraction for recording application metrics (counters, timers, gauges)
  - Used by fail-closed error handling to track infrastructure failures
  - Standard metric names: permission.check.duration, permission.check.infrastructure_failure, auth.login.success/failure
- `core/shared/src/main/scala/works/iterative/core/metrics/NoOpMetricsService.scala`
  - Test implementation (no-op, returns ZIO.unit)
  - Production implementation (ZIO Metrics/Micrometer) added in Phase 5
- `core/shared/src/main/scala/works/iterative/core/config/ConfigValidator.scala`
  - Validates all configuration on startup (ENV vars, enum parsing, combinations)
  - Collects ALL errors before failing (better UX than one-at-a-time)
  - Validates: AUTH_PROVIDER, PERMISSION_SERVICE, environment checks, OIDC requirements
- `core/shared/src/main/scala/works/iterative/core/audit/AuditLogService.scala`
  - Security audit trail for authentication and authorization events
  - Structured logging: timestamp, userId, eventType, resource, action, result, reason
  - Methods: logPermissionCheck, logAuthenticationEvent
- `core/shared/src/main/scala/works/iterative/core/audit/InMemoryAuditLogService.scala`
  - Test implementation with Ref[List[AuditEvent]]
  - Production implementation (separate audit stream) added in Phase 5

**Modify:**
- `core/shared/src/main/scala/works/iterative/core/auth/PermissionService.scala`
  - Add method: `listAllowed(subj: UserInfo, action: PermissionOp, namespace: String): UIO[Set[String]]`
  - Enables efficient reverse lookup for queries

**Create (continued in Application Layer):**
- `core/shared/src/main/scala/works/iterative/core/auth/service/TestAuthenticationService.scala`
  - Class: `TestAuthenticationService` with predefined test users
  - Methods: loginAs(userId), pre-configured test users (admin, user)
  - Uses **SCALA 3 REQUIRED** AuthProvider enum
  - ZLayer factory for easy integration

**Modify (Example):**
- Service layer methods to use Authorization helpers
- Replace role-based checks with permission-based checks
- Example pattern:
  ```scala
  def deleteDocument(id: DocId): ZIO[CurrentUser & PermissionService, AppError | AuthenticationError, Unit] =
      val target = PermissionTarget.unsafe("document", id.value)
      Authorization.require(PermissionOp("delete"), target) {
          repository.delete(id)
      }
  ```

### Infrastructure Layer

**Create:**
- `core/jvm/src/main/scala/works/iterative/core/auth/PermissionRepository.scala`
  - Trait: `PermissionRepository` with CRUD operations for relation tuples
  - Methods: hasPermission, addRelation, removeRelation, getUserRelations, getObjectRelations
- `core/jvm/src/main/scala/works/iterative/core/auth/MongoPermissionRepository.scala`
  - MongoDB implementation of PermissionRepository
  - Efficient queries using compound indexes
- `core/jvm/src/main/scala/works/iterative/core/auth/DatabasePermissionService.scala`
  - **IMPERATIVE SHELL**: Orchestrates database effects via PermissionRepository
  - Calls pure `PermissionLogic` functions for actual checking
  - Production-ready with proper error handling (fail closed with logging)
  - Uses **SCALA 3 REQUIRED** PermissionServiceType enum
- `server/http/src/main/scala/works/iterative/server/http/impl/pac4j/Pac4jAuthenticationAdapter.scala`
  - Class: `Pac4jAuthenticationAdapter extends AuthenticationService`
  - Maps Pac4J CommonProfile to BasicProfile
  - Integrates with FiberRefAuthentication
  - Uses **SCALA 3 REQUIRED** AuthProvider enum

**Modify:**
- `server/http/src/main/scala/works/iterative/server/http/impl/pac4j/Pac4jModuleRegistry.scala`
  - Integrate Pac4jAuthenticationAdapter with existing Pac4j middleware
  - Ensure user context flows from Pac4J → AuthenticationService → CurrentUser

### Presentation Layer

**Modify:**
- HTTP4S route handlers to use Authorization.require/withPermission
- Ensure Pac4J middleware calls AuthenticationService.loggedIn after successful authentication
- Error handling for AuthenticationError (401) and permission denied (403)

**Authorization Enforcement (Dual Approach):**

**Option A: Authorization Middleware (Standard)**
- Apply middleware to routes requiring authentication
- Runtime enforcement with flexible error handling
- Suitable for most applications

**Option B: Typed Route Protection (Optional)**
- Compile-time enforced route protection using **SCALA 3 REQUIRED** RouteProtection enum
- Type-safe guarantee that protected routes require authentication
- More rigid but prevents accidental unprotected routes
- Applications can choose between middleware (flexible) or typed routes (safe)

**Example Integration (Middleware):**
```scala
case req @ PUT -> Root / "documents" / DocIdVar(id) =>
    (for {
        content <- req.as[String]
        service <- ZIO.service[DocumentService]
        _ <- service.updateDocument(id, content)  // Uses Authorization.require internally
        response <- Ok()
    } yield response).catchAll {
        case _: AuthenticationError => Unauthorized()
        case e: AuthenticationError if e.userMessage.key.contains("forbidden") => Forbidden()
        case e => InternalServerError(e.getMessage)
    }
```

## Testing Strategy

### Unit Tests

**Permission Logic:**
- `PermissionConfigSpec` - Test namespace configuration and computed relations
- `InMemoryPermissionServiceSpec` - Test direct permissions, computed permissions, edge cases
- `PermissionTargetSpec` - Test parsing, validation, namespace/value/rel extraction
- `RelationTupleSpec` - Test equality, hashing

**User Model:**
- `BasicProfileSpec` - Test profile creation, claim extraction
- `CurrentUserSpec` - Test ZIO service integration, conversion to UserHandle

**Input Validation:**
- `DomainTypesSpec` - **CRITICAL: Comprehensive PermissionTarget validation tests**
  - Test all valid formats (lowercase namespace, underscores, alphanumeric objectId)
  - Test all rejection cases (uppercase, special chars, missing colon, empty parts, length violations)
  - Test edge cases (exactly 50 char namespace, boundary conditions)
  - Validates that invalid input cannot create PermissionTarget instances

**Authorization Helpers:**
- `AuthorizationSpec` - Test require, withPermission, check, filterAllowed
- Use TestAuthenticationService and AlwaysAllowPermissionService for isolation
- Test both success and failure cases (permission granted/denied)

**Pure Functions (PermissionLogic):**
- **FUNCTIONAL CORE**: Test `PermissionLogic` object methods as pure functions
- `isAllowed` tests with various relation tuple sets and configs
- `listAllowed` tests for reverse permission lookup
- `resolveComputedRelations` tests for permission inheritance
- No mocking required - pure input/output testing
- Fast, deterministic unit tests

### Integration Tests

**Authentication Flow:**
- `Pac4jAuthenticationAdapterSpec` - Test Pac4J profile mapping to BasicProfile
- Test AuthenticationService.provideCurrentUser integration with CurrentUser
- Test FiberRef lifecycle management (using ZIO.scoped to prevent memory leaks)
- Test FiberRef isolation between concurrent requests (user A doesn't see user B's context)

**Repository Integration:**
- `MongoPermissionRepositorySpec` - Test CRUD operations for relation tuples
- Test compound index usage for efficient queries
- Test listAllowed reverse lookup queries
- Test concurrent access and consistency

**Database Queries:**
- `PermissionAwareRepositorySpec` - Test two-phase query pattern
- Compare performance of naive filter vs. reverse lookup
- Test pagination with permission filtering

**End-to-End Service Tests:**
- Test complete flow: HTTP request → authentication → authorization → business logic
- Use TestAuthenticationService and InMemoryPermissionService for speed
- Test unauthorized access returns 401, forbidden returns 403

### End-to-End Tests

**User Scenarios:**
- **Scenario 1: OIDC login and access protected resource**
  - Login via Pac4J OIDC
  - Access resource user has permission for (succeeds)
  - Access resource user lacks permission for (403 Forbidden)
- **Scenario 2: Test authentication login and resource access**
  - Login as test user via TestAuthenticationService
  - Verify user context available throughout request
  - Verify correct permissions checked
- **Scenario 3: Permission inheritance**
  - User has "owner" relation to document
  - User can "edit" document (computed via inheritance)
  - User can "view" document (computed via inheritance)
- **Scenario 4: List resources with permissions**
  - Create multiple documents with different owners
  - List documents as user A (sees only permitted documents)
  - List documents as user B (sees different set)
  - Verify efficient query patterns used (no N+1)

**Error Cases:**
- Request without authentication → 401 Unauthorized
- Authenticated user without permission → 403 Forbidden
- Invalid permission target → 400 Bad Request
- Database error in permission check → 500 (fail closed)

**Performance Tests:**
- Measure permission check latency (target: < 10ms for in-memory, < 50ms for database)
- Measure reverse lookup query performance for 1000, 10000, 100000 relations
- Verify compound indexes used (explain plan analysis)

## Documentation Requirements

- [x] **Code documentation** 
  - Inline comments for complex permission checking logic
  - Scaladoc for all public methods in PermissionService, AuthenticationService
  - Examples in Authorization helper object documentation
  - PURPOSE comments in all new files (as per project conventions)

- [x] **API documentation**
  - Document PermissionService.isAllowed and listAllowed methods
  - Document Authorization.require, withPermission, check, filterAllowed
  - Document TestAuthenticationService.loginAs for testing
  - Document PermissionConfig namespace configuration format

- [ ] **Architecture decision record**
  - **CLARIFY**: Should we create ADR for choosing Zanzibar model over RBAC?
  - **CLARIFY**: Should we document decision to keep Pac4J vs. replace entirely?
  - Document permission namespace configuration approach
  - Document two-phase query pattern for efficient filtering

- [x] **User-facing documentation**
  - No UI changes, so no user-facing docs needed
  - Developer documentation for using Authorization helpers

- [ ] **Migration guide**
  - **CLARIFY**: Do we need migration guide if this is additive (no breaking changes)?
  - If existing services use role-based checks, document how to migrate to permission-based
  - Document how to configure permission namespaces for new domains
  - Examples of converting `if user.hasRole("admin")` to `Authorization.require`

## Deployment Considerations

### Database Changes

**New Collection: `permissions`**
```javascript
// MongoDB schema
{
  _id: ObjectId,
  userId: String,        // Subject ID
  namespace: String,     // e.g., "document", "folder"
  objectId: String,      // e.g., "123", "abc"
  relation: String,      // e.g., "owner", "editor", "viewer"
  createdAt: Date
}

// Indexes
db.permissions.createIndex({ userId: 1, namespace: 1, relation: 1 }, { name: "idx_user_lookup" })
db.permissions.createIndex({ namespace: 1, objectId: 1, relation: 1 }, { name: "idx_object_lookup" })
db.permissions.createIndex({ userId: 1, namespace: 1, objectId: 1, relation: 1 }, { unique: true, name: "idx_unique_permission" })
```

**Migration Strategy:**
- New collection, no changes to existing collections
- Initial deployment: InMemoryPermissionService (no database required)
- Phase 2: Add MongoDB collection with indexes
- Phase 3: Switch to DatabasePermissionService

**Data Transformation:**
- If existing data has ownership/permissions in document fields, create migration script
- Script reads documents, creates relation tuples, inserts into permissions collection
- **CLARIFY**: What is the current permission model in existing documents? Need to inventory.

### Configuration Changes

**Environment Variables:**
```bash
# Authentication mode
AUTH_PROVIDER=oidc|test

# Test authentication (when AUTH_PROVIDER=test)
TEST_AUTH_DEFAULT_USER=test-user

# OIDC configuration (when AUTH_PROVIDER=oidc)
OIDC_URL_BASE=https://auth.example.com
OIDC_CLIENT_ID=myapp
OIDC_CLIENT_SECRET=secret
OIDC_DISCOVERY_URI=https://auth.example.com/.well-known/openid-configuration

# Permission service
PERMISSION_SERVICE=memory|database
```

**Configuration File (application.conf or similar):**
```hocon
auth {
  provider = ${?AUTH_PROVIDER}  // Default: oidc in production, test in development
  
  test {
    defaultUser = ${?TEST_AUTH_DEFAULT_USER}
  }
  
  oidc {
    urlBase = ${?OIDC_URL_BASE}
    clientId = ${?OIDC_CLIENT_ID}
    clientSecret = ${?OIDC_CLIENT_SECRET}
    discoveryUri = ${?OIDC_DISCOVERY_URI}
  }
}

permissions {
  service = ${?PERMISSION_SERVICE}  // Default: memory in dev, database in production
  
  namespaces {
    document {
      implications {
        owner = ["edit", "view", "delete"]
        editor = ["view", "edit"]
        viewer = ["view"]
      }
    }
    folder {
      implications {
        owner = ["edit", "view", "delete", "manage"]
        editor = ["view", "edit"]
        viewer = ["view"]
      }
    }
  }
}
```

### Deployment Strategy

**Phase 1: Development/Testing (Week 1-2)**
- Deploy with AUTH_PROVIDER=test and PERMISSION_SERVICE=memory
- No database changes required
- No external dependencies (OIDC)
- Enables rapid development and testing

**Phase 2: Staging (Week 3-4)**
- Deploy with AUTH_PROVIDER=oidc and PERMISSION_SERVICE=memory
- Test OIDC integration with real provider
- Still using in-memory permissions (no database yet)
- Validate Pac4J integration works correctly

**Phase 3: Production Preparation (Week 5-6)**
- Add permissions collection to MongoDB
- Create indexes (do during low-traffic window)
- Deploy with PERMISSION_SERVICE=database
- Monitor performance of permission checks

**Phase 4: Production (Week 7+)**
- Full deployment with AUTH_PROVIDER=oidc and PERMISSION_SERVICE=database
- Monitor authentication and authorization metrics
- Set up alerts for permission check latency > 100ms
- Document runbook for troubleshooting auth issues

**Feature Flag (Optional):**
```scala
// If we want gradual rollout
val usePermissionBasedAuth = config.getBoolean("features.permissionBasedAuth")

if (usePermissionBasedAuth) {
    Authorization.require(op, target)(effect)
} else {
    // Old role-based check
    if (user.hasRole("admin")) effect else ZIO.fail(Forbidden)
}
```

### Rollback Plan

**If Issues with Production Deployment:**

1. **Immediate Rollback (< 5 minutes)**
   - Revert to previous version without authentication/authorization changes
   - No data loss (new permissions collection is additive)
   - Existing role-based checks continue working

2. **Partial Rollback (5-30 minutes)**
   - Switch PERMISSION_SERVICE from "database" back to "memory"
   - Keeps new code but uses simpler implementation
   - Useful if database performance issues

3. **Emergency Fallback (30+ minutes)**
   - Deploy AlwaysAllowPermissionService (allows all access)
   - **SECURITY WARNING**: Only for emergency to restore service
   - **CLARIFY**: Is this acceptable or should we fail closed instead?
   - Must be monitored and reverted quickly

4. **Data Rollback**
   - If permissions collection corrupted: Drop and recreate indexes
   - If need to revert entirely: Drop permissions collection
   - No impact on existing data (separate collection)

**Monitoring for Rollback Decision:**
- Permission check latency > 500ms (p95) sustained for 5 minutes
- Authentication failure rate > 10% (excluding invalid credentials)
- HTTP 5xx error rate increase > 50% after deployment
- User reports of access denied to owned resources

## Complexity Assessment

**Estimated Effort:** 54 hours (spread across 5 phases)

**Breakdown:**
- **Phase 1 (Permission Foundation + Infrastructure)**: 14 hours (updated from 10 hours)
  - RelationTuple, PermissionConfig: 2 hours
  - PermissionLogic (pure functions): 3 hours
  - InMemoryPermissionService implementation: 3 hours
  - Authorization helper object: 2 hours
  - **Metrics infrastructure**: 2 hours
  - **Configuration validation**: 2 hours
  - Unit tests: included in above

- **Phase 2 (Pac4J integration + TestAuthenticationService)**: 10 hours (unchanged)
  - Pac4jAuthenticationAdapter: 4 hours
  - TestAuthenticationService: 2 hours
  - Integration tests: 2 hours
  - Pac4J middleware integration: 2 hours

- **Phase 3 (Authorization guards + service migration)**: 14 hours (unchanged)
  - Update one service to use Authorization helpers: 3 hours
  - HTTP4S error handling integration: 2 hours
  - Authorization middleware (Task 6A): 2 hours
  - Typed route protection (Task 6B): 3 hours
  - End-to-end tests: 2 hours
  - Documentation and examples: 2 hours

- **Phase 4 (DatabasePermissionService + Audit Logging)**: 13 hours (updated from 11 hours)
  - PermissionRepository interface and MongoDB impl: 4 hours
  - DatabasePermissionService with fail-closed pattern: 3 hours
  - **Audit logging infrastructure**: 2 hours
  - Database migration script: 1 hour
  - Integration and performance tests: 3 hours

- **Phase 5 (Optimization + production readiness)**: 3 hours (updated from 4-6 hours)
  - Efficient query patterns (two-phase lookup): 1 hour
  - Production metrics implementation: 1 hour
  - Production audit stream configuration: 1 hour

**Reasoning:**
- Building on existing abstractions reduces design time
- In-memory implementation is straightforward (pure functional logic)
- Pac4J integration is mostly mapping/adapter code
- Authorization helpers are thin wrappers around PermissionService
- Database implementation requires careful index design
- Testing is comprehensive but well-defined patterns
- Each phase delivers value independently

**Complexity Level:** Complex (40-48 hours)

- **Multiple systems**: Authentication, authorization, user management, HTTP middleware
- **Significant integration**: Pac4J, HTTP4S, ZIO, MongoDB
- **Extensive testing required**: Unit, integration, and e2e tests across all phases
- **Production considerations**: Performance, security, monitoring, rollback
- **Five distinct phases**: Each phase could be a separate task

**Risk Multipliers:**
- Unfamiliarity with Zanzibar concepts: +20% effort
- MongoDB index optimization challenges: +10% effort
- Pac4J integration issues: +15% effort
- **CLARIFY**: Is team familiar with Zanzibar/ReBAC concepts? May need learning time.

## Risks & Mitigations

### Risk 1: Performance Degradation with Permission Checks

**Likelihood:** Medium
**Impact:** High

**Description:** Every authorization check requires querying PermissionService. If implemented naively, this could add significant latency to every request, especially for list operations that check permissions on many objects.

**Mitigation:**
- Use two-phase query pattern (reverse lookup) instead of checking each object individually
- Implement efficient indexes in MongoDB (userId + namespace + relation, namespace + objectId + relation)
- Measure and monitor permission check latency (target < 10ms for memory, < 50ms for database)
- Start with InMemoryPermissionService for initial implementation (very fast, < 1ms)
- Use streaming/pagination for large result sets instead of loading everything
- Consider caching frequently-checked permissions (e.g., user's own document list)
- Profile and optimize hot paths before production deployment

### Risk 2: Complexity of Zanzibar Model for Team

**Likelihood:** Medium
**Impact:** Medium

**Description:** Relationship-based access control (ReBAC) is more complex than role-based access control. Team may struggle with concepts like relation tuples, computed relations, and permission inheritance, leading to incorrect implementations or confusion.

**Mitigation:**
- Provide comprehensive documentation with concrete examples
- Start with simple namespaces (document ownership) before complex ones
- Create Authorization helper object that abstracts complexity
- Provide working examples in one service first
- Pair programming for first few implementations
- Code review checklist for permission-related changes
- **CLARIFY**: Schedule knowledge-sharing session on Zanzibar concepts?
- Keep role-based option available temporarily during transition

### Risk 3: Pac4J Integration Issues

**Likelihood:** Low-Medium
**Impact:** Medium

**Description:** Pac4J is Java-based and may have subtle integration issues with Scala/ZIO code. Profile mapping from CommonProfile to BasicProfile might lose information or have edge cases.

**Mitigation:**
- Thoroughly test Pac4jAuthenticationAdapter with various profile types
- Log all profile mappings during development to catch missing fields
- Handle null values from Java code defensively (use Option everywhere)
- Test with real OIDC provider early (don't wait for production)
- Keep Pac4J integration isolated in adapter (easy to replace if needed)
- Maintain fallback TestAuthenticationService for development

### Risk 4: Database Migration Complexity

**Likelihood:** Medium
**Impact:** Medium

**Description:** If existing documents have embedded ownership/permission data, migrating to relation tuples could be complex and error-prone. Inconsistencies between old and new models during transition period.

**Mitigation:**
- **CLARIFY**: Inventory existing permission model in documents
- Create migration script that can be run idempotently
- Test migration on staging data before production
- Keep old permission fields during transition (dual write, eventually remove)
- Verify data consistency after migration (count tuples vs. old records)
- Deploy InMemoryPermissionService first (no migration needed)
- Plan migration as separate phase after initial implementation validated

### Risk 5: Authorization Bypass Bugs

**Likelihood:** Low
**Impact:** Critical

**Description:** Bugs in permission checking logic could allow unauthorized access to resources. This is a security vulnerability that could expose sensitive data or allow unauthorized actions.

**Mitigation:**
- **Fail closed with observability** - Permission check failures MUST deny access while logging full context:
  ```scala
  // DatabasePermissionService pattern:
  repository.hasPermission(userId, resource)
    .tapError(error =>
      ZIO.logWarning(s"Permission check failed for user=$userId resource=$resource error=$error") *>
      recordMetric("permission.check.infrastructure_failure", error)
    )
    .catchAll(_ => ZIO.succeed(false))  // Fail closed with visibility
  ```
- Structured logging captures: userId, resource, operation, error details
- Metrics track infrastructure failures separately from access denials
- Never swallow errors silently - always log with full context before denying
- Comprehensive test coverage including negative tests (access denied cases)
- Test error scenarios: database down, network timeout, invalid data
- Code review all authorization logic with security mindset
- Use Authorization helpers consistently (don't inline permission checks)
- Integration tests verify 401/403 returned correctly
- Add audit logging for all permission checks (can review for suspicious patterns)
- Security testing / penetration testing before production
- **CLARIFY**: Is security review required before production deployment?

### Risk 6: Test Authentication Leaks to Production

**Likelihood:** Low
**Impact:** Critical

**Description:** If TestAuthenticationService is accidentally enabled in production, it would allow bypassing real authentication, giving anyone access.

**Mitigation:**
- Environment-based configuration with explicit checks
- Fail fast on startup if AUTH_PROVIDER=test in production environment
- Configuration validation: reject invalid combinations
- Alert/monitoring on authentication service type in production
- Code review for any TestAuthenticationService usage
- Integration tests verify correct AuthenticationService loaded per environment

### Risk 7: FiberRef Context Loss

**Likelihood:** Low-Medium
**Impact:** High

**Description:** FiberRef-based user context could be lost during fiber boundaries (ZIO.async, race conditions, improper context passing), leading to unexpected authentication failures.

**Mitigation:**
- Thoroughly test FiberRefAuthentication with concurrent requests
- Test context isolation between fibers (user A shouldn't see user B's context)
- Use AuthenticationService.provideCurrentUser consistently
- Add logging to track when context is established/lost
- Integration tests with concurrent requests to verify isolation
- Consider ZIO environment layer as alternative if FiberRef issues persist
- Document proper usage patterns for developers

## Dependencies

### Prerequisites

**Must exist before starting implementation:**
- [x] Research completed (research.md)
- [x] PermissionService interface exists
- [x] UserInfo/BasicProfile hierarchy exists
- [x] CurrentUser ZIO service exists
- [x] AuthenticationService interface exists
- [x] Pac4J integration exists in server/http module
- [x] MongoDB repository infrastructure exists
- [ ] **CLARIFY**: MongoDB connection pooling and error handling patterns documented?
- [ ] **CLARIFY**: Project-specific permission namespaces identified (document, folder, what else)?

### Blocked By

**Other issues that must be resolved first:**
- None identified - this work can proceed independently

**Potential blockers:**
- **CLARIFY**: Are there any ongoing refactorings to auth module that would conflict?
- **CLARIFY**: Is MongoDB schema migration process established?
- **CLARIFY**: Are there environment configuration changes that need approval?

### Blocks

**Issues that depend on this completion:**
- Any feature requiring resource-level permissions (can't implement without PermissionService)
- Any feature requiring ownership-based access control
- Any feature requiring permission delegation or sharing
- Admin UI for managing permissions (needs PermissionService implemented first)
- Audit logging for access control (needs permission checks to log)
- **CLARIFY**: Are there specific features waiting on this? Need to coordinate.

## Open Questions

- [x] **Q1: What is the current permission model in existing documents?**
  - Need to inventory: Do documents have owner_id field? Permission arrays? Role-based only?
  - Impacts migration complexity for Phase 4

- [x] **Q2: What are all the permission namespaces we need?**
  - Identified so far: "document", "folder"
  - **CLARIFY**: What other domain objects need permissions? (projects, teams, organizations?)
  - Need complete list to design PermissionConfig upfront

- [x] **Q3: Is security review required before production?**
  - This is authentication/authorization code - typically requires security review
  - **CLARIFY**: What is the security review process? Who needs to approve?

- [x] **Q4: Should we fail open or fail closed on permission check errors?**
  - Recommendation: Fail closed (deny access on error)
  - **CLARIFY**: Confirm this is acceptable for production

- [x] **Q5: Is the team familiar with Zanzibar/ReBAC concepts?**
  - Impacts learning curve and effort estimates
  - **CLARIFY**: Do we need training/knowledge-sharing session before implementation?

- [x] **Q6: Should we create ADRs for key decisions?**
  - Zanzibar model vs. RBAC
  - Keep Pac4J vs. replace
  - **CLARIFY**: What is ADR process/requirements?

- [x] **Q7: How should we handle emergency access restoration?**
  - If permission system fails, do we fail closed (no access) or have emergency override?
  - **CLARIFY**: Is AlwaysAllowPermissionService acceptable as temporary emergency measure?

- [x] **Q8: What monitoring/alerting is needed?**
  - Permission check latency thresholds?
  - Authentication failure rate alerts?
  - **CLARIFY**: What metrics should we track? What are alert thresholds?

- [x] **Q9: Are there any other systems/teams that need to be coordinated with?**
  - Frontend team (for handling 401/403 responses)?
  - Operations team (for MongoDB setup)?
  - **CLARIFY**: Who needs to be involved in planning/deployment?

## Workflow Recommendation

Based on the analysis, this feature is best implemented using:

**Recommended: API-First**

**Rationale:**
- **Clear contracts exist**: PermissionService, AuthenticationService interfaces already defined
- **Backend-heavy work**: Most complexity is in permission checking, database queries, Pac4J integration
- **No UI component**: This is purely backend authentication/authorization infrastructure
- **Testable independently**: Can fully test authorization logic without UI
- **Foundational**: Other features will depend on this working correctly
- **Admin tools**: Permission management will need admin endpoints before any UI

**API-First Implementation Plan:**
1. Implement InMemoryPermissionService with comprehensive unit tests
2. Implement Authorization helpers with integration tests
3. Create test endpoints to verify permission checking (POST /api/admin/permissions/check)
4. Implement TestAuthenticationService for development
5. Integrate Pac4J with AuthenticationService
6. Create admin endpoints for permission management (CRUD relation tuples)
7. Implement DatabasePermissionService with performance tests
8. Deploy backend, then coordinate with frontend for proper 401/403 handling

**Why Not UI-First:**
- No user-facing UI for this feature (purely backend infrastructure)
- Permission checks happen server-side, invisible to users
- Admin UI for permission management is a separate feature, depends on API existing first

---

**Analysis Status:** Ready for Review

**Next Steps:**
1. Review this analysis for accuracy and completeness
2. Address CLARIFY questions with stakeholders
3. Run `/create-tasks IWSD-74` to generate implementation plan
4. Run `/review-tasks IWSD-74` for implementation plan validation
5. Begin Phase 1 implementation (InMemoryPermissionService + Authorization helpers)
