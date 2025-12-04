# Implementation Log: IWSD-74

## 2025-12-04: Planning Cleanup and Phase 5 Addition

**Decision:** Remove PermissionServiceFactory, add Phase 5 for DatabasePermissionService.

**Context:**
- Phase 4 was marked "complete" but DatabasePermissionService was never implemented
- PermissionServiceFactory in core/jvm had a `die` for the database case - essentially a stub
- The factory pattern is unnecessary since applications wire layers directly via ZLayer

**Files Removed:**
- `core/jvm/src/main/scala/works/iterative/core/auth/PermissionServiceFactory.scala` - Unnecessary factory
- `core/jvm/src/test/scala/works/iterative/core/auth/PermissionServiceFactorySpec.scala` - Tests for removed code

**Files Added/Changed:**
- `project-management/issues/IWSD-74/tasks.md` - Updated to 5 phases, clarified Phase 4 scope, added Phase 5
- `project-management/issues/IWSD-74/phase-05.md` - New phase for DatabasePermissionService implementation

**Phase Structure Update:**
- Phase 4 renamed to "Audit Logging Infrastructure" (what was actually completed)
- Phase 5 added for "Database Persistence (Production PermissionService)"

**Architecture Decision:**
- DatabasePermissionService will query database on each permission check (not pre-load)
- This enables frontend usage (can't pre-load entire permission table into SPA)
- Fail-closed error handling: database errors return `false`, logged with full context
- Repository pattern: PermissionRepository trait in sqldb, implementations in sqldb-postgresql/sqldb-mysql

---

## 2025-12-04: Replace Custom Config Validation with ZIO Config

**Decision:** Replace custom `ConfigValidator` and `ConfigLoader` with native ZIO Config.

**Context:**
- `ConfigLoader.scala` was dead code - defined but never called
- `ConfigValidator.scala` implemented manual validation logic with mutable collections
- The code followed FCIS but was never integrated into the application
- ZIO Config provides the same functionality with less code and better integration

**Files Removed:**
- `core/jvm/src/main/scala/works/iterative/infrastructure/config/ConfigLoader.scala` - Dead code, never used
- `core/shared/src/main/scala/works/iterative/core/config/ConfigValidator.scala` - Manual validation replaced by ZIO Config
- `core/jvm/src/test/scala/works/iterative/core/config/ConfigValidatorSpec.scala` - Tests for removed code

**Files Added/Changed:**
- `core/shared/src/main/scala/works/iterative/core/config/AuthConfig.scala` - ZIO Config-based auth configuration with:
  - `AuthProvider` enum (Oidc, Test) with `configDescriptor`
  - `PermissionServiceType` enum (Memory, Database) with `configDescriptor`
  - `Environment` enum (Development, Production) with `configDescriptor`
- `core/jvm/src/test/scala/works/iterative/core/config/AuthConfigSpec.scala` - Comprehensive tests for config descriptors
- `server/http/src/main/scala/works/iterative/server/http/AuthenticationServiceFactory.scala` - Consolidated to use shared `AuthProvider` enum and ZIO Config (removed duplicate enum)
- `server/http/src/test/scala/works/iterative/server/http/AuthenticationServiceFactorySpec.scala` - Updated tests for ZIO Config

**Note:** PermissionServiceFactory was also updated in this commit but later removed (see 2025-12-04 Planning Cleanup entry above).

**Configuration Keys:**
- `permission_service`: "memory" | "database"
- `auth_provider`: "oidc" | "test"
- `env`: "development" | "production" (defaults to "development" if not set)

**Benefits:**
- No custom config loading code to maintain
- Standard ZIO Config patterns (works with env vars, HOCON, etc.)
- Built-in validation and error messages
- Consistent with other config in the codebase (e.g., `SMTPConfig`)
- ~150 lines of dead/manual code removed

**Trade-offs:**
- Cross-validation (e.g., "test auth forbidden in production") now happens at service construction time rather than config loading time
- Simpler but slightly less centralized validation

**Verification:**
- All `core.jvm.test` tests pass (136 tests)
- All modules compile successfully
