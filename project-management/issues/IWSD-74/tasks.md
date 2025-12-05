# Implementation Tasks: Investigate authentication for HTTP4S server

**Issue:** IWSD-74
**Total Phases:** 5
**Completed Phases:** 5/5
**Complexity:** Complex
**Estimated Total Time:** 54 hours (updated from 48 hours, +6 hours for infrastructure)
**Generated:** 2025-11-13
**Updated:** 2025-11-15 (Applied critical issue fixes + infrastructure tasks)

## Overview

Implement production-ready authentication and authorization system for HTTP4S server by building on existing well-designed abstractions (PermissionService interface, user model hierarchy, AuthenticationService, CurrentUser). This enables pluggable authentication (OIDC for production, test mode for development), relationship-based permissions (Zanzibar-inspired ReBAC), and declarative authorization guards throughout the ZIO application.

## Implementation Strategy

Build incrementally across 4 phases, starting with in-memory implementations for immediate value, then integrating authentication providers, adding authorization guards to services, and implementing database persistence. Each phase delivers working functionality that can be tested independently. The TDD approach ensures comprehensive test coverage and prevents authorization bypass bugs (critical security concern).

**Architectural Note:** This implementation follows FCIS (Functional Core, Imperative Shell) principles by extracting pure domain logic (PermissionLogic) from effect-based orchestration (PermissionService, Authorization helpers). Effect-based code is recognized as application-layer infrastructure, not core domain.

## Phases

### Phase 1: Permission Foundation (In-Memory Implementation)
- [x] [phase-complete] Phase implementation and review complete
- **Objective:** Create working permission system with in-memory storage, enabling immediate testing of Zanzibar-inspired ReBAC model without database dependencies. Also establish metrics and configuration infrastructure.
- **Estimated Time:** 14 hours (updated from 10 hours, +4 hours for metrics and config validation)
- **Tasks:** 11 tasks (10 implementation + phase success criteria) - See [phase-01.md](./phase-01.md)
- **Prerequisites:** None (foundational phase)

### Phase 2: Authentication Integration (Pac4J & Test Mode)
- [x] [phase-complete] Phase implementation and review complete
- **Objective:** Bridge Pac4J OIDC integration with AuthenticationService interface and create test authentication mode for rapid development without OIDC dependencies.
- **Estimated Time:** 10 hours (updated from 9 hours)
- **Tasks:** 4 tasks - See [phase-02.md](./phase-02.md)
- **Prerequisites:** Completion of Phase 1 (Authorization helpers exist for integration testing)

### Phase 3: Authorization Guards & Service Integration
- [x] [phase-complete] Phase implementation and review complete
- **Objective:** Integrate authorization guards into HTTP4S routes and service layer, demonstrating declarative permission checking with proper error handling (401/403). Provide both middleware-based (standard) and typed route (optional) approaches for authorization enforcement.
- **Estimated Time:** 14 hours (updated from 11 hours)
- **Tasks:** 8 tasks - See [phase-03.md](./phase-03.md)
- **Prerequisites:** Completion of Phase 2 (AuthenticationService and CurrentUser available)

### Phase 4: Audit Logging Infrastructure
- [x] [phase-complete] Phase implementation and review complete
- **Objective:** Create audit logging infrastructure for tracking permission checks and authentication events.
- **Estimated Time:** 4 hours
- **Tasks:** 2 tasks (AuditLogService + InMemoryAuditLogService) - See [phase-04.md](./phase-04.md)
- **Prerequisites:** Completion of Phase 3
- **Note:** PermissionServiceFactory was removed - applications wire layers directly

### Phase 5: Database Persistence (Production PermissionService)
- [x] [phase-complete] Phase implementation and review complete
- **Objective:** Implement production-ready permission storage using a database, enabling persistent permissions that survive server restarts and scale beyond in-memory limits.
- **Estimated Time:** 10 hours
- **Tasks:** See [phase-05.md](./phase-05.md)
- **Prerequisites:** Completion of Phase 4 (Audit logging infrastructure available)

## Testing Strategy

### Unit Tests (Per Phase)
- Test all domain models (RelationTuple, PermissionConfig, etc.)
- Test pure functions in PermissionLogic (without ZIO)
- Test effect-based implementations (InMemoryPermissionService, Authorization helpers)
- Use ZIO Test framework with `mill core.shared.test` and `mill core.jvm.test`

### Integration Tests (Phase 3)
- Test authorization guards in service layer
- Test HTTP error mapping (401/403)
- Test end-to-end authorization workflows
- Use TestAuthenticationService for controllable user context

### Property-Based Tests
- Test permission inheritance rules with random relation tuples (100 samples)
- Verify listAllowed returns correct subset for any permission configuration

### Test Pyramid
```
     /\
    /  \  E2E Tests (manual validation, Phase 5)
   /----\
  /      \  Integration Tests (service + HTTP layer, Phase 3)
 /--------\
/          \  Unit Tests (all phases, comprehensive coverage)
```

## Documentation Requirements

1. **AUTHORIZATION_GUIDE.md** (Phase 3) - ✅ Completed
   - Authorization.require pattern with code examples
   - Authorization.filterAllowed pattern for list queries
   - Permission target format (namespace:objectId) with validation rules
   - Permission operations (view, edit, delete, create)
   - Configuring permission namespaces (PermissionConfig)
   - Testing with TestAuthenticationService and InMemoryPermissionService
   - Tapir endpoint integration patterns
   - Error handling flow (AuthErrorHandler mapping)

2. **API Documentation** (Phase 4)
   - Scaladoc for all public APIs
   - Usage examples in documentation comments

3. **Configuration Guide** (Phase 5)
   - Environment variables (AUTH_PROVIDER, PERMISSION_SERVICE, OIDC_*)
   - Default configurations for development vs production

## Deployment Checklist

- [ ] Configuration validated (AUTH_PROVIDER, PERMISSION_SERVICE, ENV)
- [ ] Database migrations applied (if using DatabasePermissionService)
- [ ] OIDC credentials configured (if using AUTH_PROVIDER=oidc)
- [ ] Metrics collection verified (permission.check.duration, auth.login.*)
- [ ] Audit logs streaming to separate log file/service
- [ ] All tests pass: `mill __.test`
- [ ] Security review: Permission checks fail-closed on errors
- [ ] Performance test: listAllowed queries optimized for large datasets

---

**Next Steps:** Begin with Phase 5 - see [phase-05.md](./phase-05.md)
