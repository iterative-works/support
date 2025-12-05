# Phase 5: Database Persistence (Production PermissionService)

**Issue:** IWSD-74
**Phase:** 5 of 5
**Objective:** Implement production-ready permission storage using a database, enabling persistent permissions that survive server restarts and scale beyond in-memory limits.
**Estimated Time:** 10 hours
**Prerequisites:** Completion of Phase 4 (Audit logging infrastructure available)

## Phase Objective

Implement database-backed `PermissionService` that:

1. **Persists RelationTuples** to SQL database (PostgreSQL or MySQL)
2. **Fail-closed error handling** - Database errors result in access denial (return `false`), not security bypass
3. **Observability** - Log infrastructure failures with full context before denying
4. **Efficient queries** - Compound indexes for fast permission checks and reverse lookups
5. **Audit integration** - Log permission checks via AuditLogService

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    PermissionService                         │
│                   (trait in core/shared)                     │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────┐
│ InMemory        │ │ Database        │ │ AlwaysAllow         │
│ PermissionSvc   │ │ PermissionSvc   │ │ PermissionSvc       │
│ (core/shared)   │ │ (sqldb)         │ │ (core/shared)       │
└─────────────────┘ └─────────────────┘ └─────────────────────┘
                              │
                              ▼
              ┌─────────────────────────────────────┐
              │    PermissionRepository (trait)      │
              │          (sqldb module)              │
              └─────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│ PostgreSQL              │     │ MySQL                   │
│ PermissionRepository    │     │ PermissionRepository    │
│ (sqldb-postgresql)      │     │ (sqldb-mysql)           │
└─────────────────────────┘     └─────────────────────────┘
```

**Key Design Decisions:**
- `DatabasePermissionService` lives in `sqldb` module (can depend on repository)
- Repository implementations live in database-specific modules
- Applications wire the appropriate implementation via ZLayer
- No factory needed - layer composition handles selection
- Database queries on each permission check (not pre-loaded) - enables frontend usage

## Tasks

### Task 1: Create PermissionRepository trait (TDD Cycle)

**RED - Write Failing Test:**
- [x] [impl] Create test file: `sqldb/src/test/scala/works/iterative/sqldb/PermissionRepositorySpec.scala`
- [x] [impl] Write test case for `hasRelation(userId, relation, target)` returning true when exists
- [x] [impl] Write test case for `hasRelation` returning false when not exists
- [x] [impl] Write test case for `addRelation` followed by `hasRelation` returning true
- [x] [impl] Write test case for `removeRelation` followed by `hasRelation` returning false
- [x] [impl] Write test case for `getUserRelations(userId, namespace)` returning all relations
- [x] [impl] Run test: `./mill sqldb.test`
- [x] [impl] Verify tests fail with "trait PermissionRepository not found"

**GREEN - Make Test Pass:**
- [x] [impl] Create file: `sqldb/src/main/scala/works/iterative/sqldb/PermissionRepository.scala`
- [x] [impl] Add PURPOSE comments explaining permission tuple persistence
- [x] [impl] Define trait:
  ```scala
  trait PermissionRepository:
    def hasRelation(userId: UserId, relation: String, target: PermissionTarget): Task[Boolean]
    def addRelation(userId: UserId, relation: String, target: PermissionTarget): Task[Unit]
    def removeRelation(userId: UserId, relation: String, target: PermissionTarget): Task[Unit]
    def getUserRelations(userId: UserId, namespace: String): Task[Set[RelationTuple]]
  ```
- [x] [impl] Create in-memory test double for spec validation
- [x] [impl] Run test: `./mill sqldb.test`
- [x] [impl] Verify all tests pass

**Success Criteria:** PermissionRepository trait defines persistence contract for relation tuples

### Task 2: Create DatabasePermissionService (TDD Cycle)

**RED - Write Failing Test:**
- [x] [impl] Create test file: `sqldb/src/test/scala/works/iterative/sqldb/DatabasePermissionServiceSpec.scala`
- [x] [impl] Write test case for `isAllowed` using stored permissions (via repository)
- [x] [impl] Write test case for `isAllowed` with permission inheritance (owner implies editor)
- [x] [impl] Write test case for `listAllowed` returning correct resources
- [x] [impl] Write test case for `grantPermission` persisting to repository
- [x] [impl] Write test case for `revokePermission` removing from repository
- [x] [impl] Write test case for database error resulting in `isAllowed` returning false (fail-closed)
- [x] [impl] Run test: `./mill sqldb.test`
- [x] [impl] Verify tests fail with "class DatabasePermissionService not found"

**GREEN - Make Test Pass:**
- [x] [impl] Create file: `sqldb/src/main/scala/works/iterative/sqldb/DatabasePermissionService.scala`
- [x] [impl] Add PURPOSE comments: Production PermissionService with database persistence
- [x] [impl] Implement class:
  ```scala
  class DatabasePermissionService(
      repository: PermissionRepository,
      config: PermissionConfig
  ) extends PermissionService
  ```
- [x] [impl] Implement `isAllowed` with fail-closed pattern:
  ```scala
  def isAllowed(subj: Option[UserInfo], action: PermissionOp, obj: PermissionTarget): UIO[Boolean] =
    subj match
      case None => ZIO.succeed(false)
      case Some(user) =>
        (for
          tuples <- repository.getUserRelations(user.subjectId, obj.namespace)
          result = PermissionLogic.isAllowed(user.subjectId, action, obj, tuples, config)
        yield result)
          .tapError(error =>
            ZIO.logWarning(s"Permission check failed: user=${user.subjectId} target=$obj error=$error")
          )
          .catchAll(_ => ZIO.succeed(false)) // Fail closed
  ```
- [x] [impl] Implement `listAllowed` using repository
- [x] [impl] Implement `grantPermission` delegating to repository.addRelation
- [x] [impl] Implement `revokePermission` delegating to repository.removeRelation
- [x] [impl] Add ZLayer factory requiring PermissionRepository and PermissionConfig
- [x] [impl] Run test: `./mill sqldb.test`
- [x] [impl] Verify all tests pass

**REFACTOR - Improve Quality:**
- [x] [impl] Add Scaladoc with fail-closed security explanation
- [x] [impl] Add logging for repository operations (at debug level)
- [x] [impl] Verify test coverage for error scenarios
- [x] [impl] Run test: `./mill sqldb.test`
- [x] [impl] Verify all tests still pass

**Success Criteria:** DatabasePermissionService implements PermissionService with fail-closed error handling

### Task 3: Implement PostgreSQL PermissionRepository (TDD Cycle)

**RED - Write Failing Test:**
- [x] [impl] Create test file: `sqldb-postgresql/src/test/scala/works/iterative/sqldb/postgresql/PostgreSQLPermissionRepositorySpec.scala`
- [x] [impl] Write integration tests using Testcontainers (follow MessageCatalogueRepositorySpec pattern)
- [x] [impl] Test `hasRelation` with real PostgreSQL
- [x] [impl] Test `addRelation` persistence across connections
- [x] [impl] Test `removeRelation` removes from database
- [x] [impl] Test `getUserRelations` with multiple tuples
- [x] [impl] Test constraint violation on duplicate relation (should be idempotent)
- [x] [impl] Run test: `./mill sqldb-postgresql.test`
- [x] [impl] Verify tests fail with "class PostgreSQLPermissionRepository not found"

**GREEN - Make Test Pass:**
- [x] [impl] Create migration: `sqldb-postgresql/src/main/resources/db/migration/postgresql/V2__create_permissions.sql`
  ```sql
  CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    relation VARCHAR(100) NOT NULL,
    namespace VARCHAR(100) NOT NULL,
    object_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_permission UNIQUE (user_id, relation, namespace, object_id)
  );

  CREATE INDEX idx_permissions_user_namespace ON permissions(user_id, namespace);
  CREATE INDEX idx_permissions_target ON permissions(namespace, object_id);
  ```
- [x] [impl] Create file: `sqldb-postgresql/src/main/scala/works/iterative/sqldb/postgresql/PostgreSQLPermissionRepository.scala`
- [x] [impl] Add PURPOSE comments
- [x] [impl] Implement using Magnum (follow existing repository patterns)
- [x] [impl] Handle constraint violations gracefully (addRelation should be idempotent)
- [x] [impl] Add ZLayer factory
- [x] [impl] Run test: `./mill sqldb-postgresql.test`
- [x] [impl] Verify all tests pass

**Success Criteria:** PostgreSQL implementation passes all integration tests

### Task 4: Implement MySQL PermissionRepository (TDD Cycle)

**RED - Write Failing Test:**
- [x] [impl] Create test file: `sqldb-mysql/src/test/scala/works/iterative/sqldb/mysql/MySQLPermissionRepositorySpec.scala`
- [x] [impl] Write integration tests using Testcontainers (follow existing MySQL test patterns)
- [x] [impl] Run test: `./mill sqldb-mysql.test`
- [x] [impl] Verify tests fail

**GREEN - Make Test Pass:**
- [x] [impl] Create migration: `sqldb-mysql/src/main/resources/db/migration/mysql/V2__create_permissions.sql`
- [x] [impl] Create file: `sqldb-mysql/src/main/scala/works/iterative/sqldb/mysql/MySQLPermissionRepository.scala`
- [x] [impl] Implement (mirror PostgreSQL implementation)
- [x] [impl] Add ZLayer factory
- [x] [impl] Run test: `./mill sqldb-mysql.test`
- [x] [impl] Verify all tests pass

**Success Criteria:** MySQL implementation passes all integration tests

### Task 5: End-to-end integration test (Validation)

**Integration Test:**
- [x] [impl] Create test: `sqldb-postgresql/src/test/scala/works/iterative/sqldb/postgresql/DatabasePermissionServiceE2ESpec.scala`
- [x] [impl] Test complete flow: grant permission → check allowed → revoke → check denied
- [x] [impl] Test permission inheritance works with database storage
- [x] [impl] Test concurrent permission checks (thread safety)
- [x] [impl] Run test: `./mill sqldb-postgresql.test`
- [x] [impl] Verify all tests pass

**Success Criteria:** Full integration verified with real database

## Phase Success Criteria

- [x] [impl] PermissionRepository trait defined in sqldb module
- [x] [impl] DatabasePermissionService implements PermissionService with fail-closed pattern
- [x] [impl] PostgreSQL implementation passes integration tests
- [x] [impl] MySQL implementation passes integration tests
- [x] [impl] All tests pass: `./mill __.test`
- [x] [reviewed] Security review: fail-closed verified
- [x] [reviewed] Phase validation approved

---

**Phase Status:** Complete
**Next Phase:** None (final phase)
