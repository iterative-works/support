# Task: Split sqldb Module into Database-Specific Modules

**Issue:** IWSD-76
**Created:** 2025-11-18
**Status:** Not Started
**Priority:** High (blocks MySQL support)

## Problem Statement

The current `sqldb` module includes PostgreSQL-specific dependencies (`magnumPG`, `flyway-database-postgresql`, `postgresql` JDBC driver) directly in the module definition. Since `support` is a library consumed by multiple projects, this forces ALL consuming projects to pull in PostgreSQL dependencies even if they use MySQL/MariaDB.

**Current structure (problematic):**
```
sqldb/
  - moduleDeps: core.jvm
  - mvnDeps: magnumPG, flyway-database-postgresql, postgresql driver
  - Contains: MessageCatalogueRepository (database-agnostic interface)
  - Contains: MessageCatalogueRepositoryImpl (PostgreSQL-specific implementation)
  - Migrations: V1__create_message_catalogue.postgresql.sql
```

**Impact:**
- Projects using MySQL are forced to include PostgreSQL deps
- Cannot support MySQL without significant refactoring
- Violates dependency minimization principle for libraries

## Decision: Option 1 - Separate Database-Specific Modules

We will split `sqldb` into three modules following the pattern:
1. **`sqldb`** - Core module with database-agnostic code
2. **`sqldb-postgresql`** - PostgreSQL-specific implementations
3. **`sqldb-mysql`** - MySQL/MariaDB-specific implementations

Projects will depend on ONE of: `sqldb-postgresql` OR `sqldb-mysql`

**Rationale:**
- Clean separation of concerns
- Projects only get dependencies they need
- Follows existing patterns (magnumPG, magnumMySQL, TestContainers modules)
- Type-safe (can't accidentally mix database implementations)
- Extensible (can add `sqldb-h2`, `sqldb-oracle` in future)

## Implementation Plan

### Phase 1: Create Module Structure

#### 1.1 Create `sqldb-postgresql` Module

**Location:** `build.mill` (add after `sqldb` module definition)

```scala
object `sqldb-postgresql` extends JvmOnlyModule {
  def moduleName = "sqldb-postgresql"
  def description = "IW Support SQL Database Library - PostgreSQL Implementation"
  def moduleDeps = Seq(sqldb)

  def mvnDeps = super.mvnDeps() ++ Seq(
    IWMillDeps.magnumPG,
    mvn"org.flywaydb:flyway-database-postgresql:11.4.0",
    mvn"org.postgresql:postgresql:42.7.5"
  )

  // Testing module for PostgreSQL
  object testing extends JvmOnlyModule {
    def moduleName = "sqldb-postgresql-testing"
    def description = "IW Support PostgreSQL Testing Library"
    def moduleDeps = Seq(`sqldb-postgresql`, sqldb.testing)

    def mvnDeps = super.mvnDeps() ++ Seq(
      mvn"org.testcontainers:postgresql:1.20.4"
    )
  }
}
```

#### 1.2 Create `sqldb-mysql` Module

**Location:** `build.mill` (add after `sqldb-postgresql` module)

```scala
object `sqldb-mysql` extends JvmOnlyModule {
  def moduleName = "sqldb-mysql"
  def description = "IW Support SQL Database Library - MySQL/MariaDB Implementation"
  def moduleDeps = Seq(sqldb)

  def mvnDeps = super.mvnDeps() ++ Seq(
    IWMillDeps.magnumMySQL,  // Check if this exists, or use magnumZIO
    mvn"org.flywaydb:flyway-database-mysql:11.4.0",
    mvn"com.mysql:mysql-connector-j:9.1.0"  // Official MySQL driver
  )

  // Testing module for MySQL
  object testing extends JvmOnlyModule {
    def moduleName = "sqldb-mysql-testing"
    def description = "IW Support MySQL Testing Library"
    def moduleDeps = Seq(`sqldb-mysql`, sqldb.testing)

    def mvnDeps = super.mvnDeps() ++ Seq(
      mvn"org.testcontainers:mysql:1.20.4"
    )
  }
}
```

#### 1.3 Update Core `sqldb` Module

**Location:** `build.mill` (modify existing `sqldb` module around line 300)

**Remove these dependencies (move to sqldb-postgresql):**
- `IWMillDeps.magnumPG`
- `mvn"org.flywaydb:flyway-database-postgresql:11.4.0"`
- `mvn"org.postgresql:postgresql:42.7.5"`

**Keep these (database-agnostic):**
- `IWMillDeps.magnumZIO` (base Magnum support)
- `mvn"org.flywaydb:flyway-core:11.4.0"` (core Flyway)
- `mvn"com.zaxxer:HikariCP:6.2.1"` (connection pooling)

```scala
object sqldb extends JvmOnlyModule {
  def moduleName = "sqldb"
  def description = "IW Support SQL Database Library - Core"
  def moduleDeps = Seq(core.jvm)

  def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore ++ Dependencies.zioJson ++
    Dependencies.zioConfig ++ Seq(
      IWMillDeps.magnumZIO,      // Keep - base Magnum
      IWMillDeps.chimney,        // Keep - transformations
      mvn"org.flywaydb:flyway-core:11.4.0",      // Keep - core
      mvn"com.zaxxer:HikariCP:6.2.1"             // Keep - pooling
      // REMOVED: magnumPG, flyway-database-postgresql, postgresql driver
    )
}
```

### Phase 2: Reorganize Source Code

#### 2.1 Keep in Core `sqldb` Module

**These are database-agnostic:**

- `sqldb/src/main/scala/works/iterative/sqldb/`
  - `MessageCatalogue.scala` (trait - interface only)
  - `MessageCatalogueRepository.scala` (trait with layer - interface)
  - `DatabaseSupport.scala` (if exists)
  - Any other database-agnostic traits/interfaces

- `sqldb/src/main/scala/works/iterative/sqldb/migration/`
  - Move to `core/jvm` if it's database-agnostic OR
  - Keep base migration logic, move DB-specific parts

#### 2.2 Move to `sqldb-postgresql` Module

**Create directory structure:**
```
sqldb-postgresql/
  src/
    main/
      scala/works/iterative/sqldb/postgresql/
      resources/
        db/migration/postgresql/           ← Database-specific directory
    test/
      scala/works/iterative/sqldb/postgresql/
      resources/
```

**Move these files:**
- `sqldb/src/main/scala/works/iterative/sqldb/MessageCatalogueRepository.scala`
  - Rename to `PostgreSQLMessageCatalogueRepository.scala`
  - Move to `sqldb-postgresql/src/main/scala/works/iterative/sqldb/postgresql/`

- Migration files:
  - `core/jvm/src/main/resources/db/migration/V1__create_message_catalogue.postgresql.sql`
  - Move to `sqldb-postgresql/src/main/resources/db/migration/postgresql/V1__create_message_catalogue.sql`
  - **Note:** Remove `.postgresql` suffix - directory path indicates database type

- Migration tool:
  - `sqldb/src/main/scala/works/iterative/sqldb/migration/MessageCatalogueMigration.scala`
  - Move to `sqldb-postgresql/src/main/scala/works/iterative/sqldb/postgresql/migration/`
  - Rename to `PostgreSQLMessageCatalogueMigration.scala`

- CLI tool:
  - `sqldb/src/main/scala/works/iterative/sqldb/migration/MessageCatalogueMigrationCLI.scala`
  - Move to `sqldb-postgresql/src/main/scala/works/iterative/sqldb/postgresql/migration/`
  - Rename to `PostgreSQLMessageCatalogueMigrationCLI.scala`

**Move test files:**
- All tests from `sqldb/src/test/scala/works/iterative/sqldb/`
  - Move to `sqldb-postgresql/src/test/scala/works/iterative/sqldb/postgresql/`
  - Update package declarations
  - Update imports

- Test resources:
  - `sqldb/src/test/resources/*`
  - Move to `sqldb-postgresql/src/test/resources/`

#### 2.3 Create `sqldb-mysql` Implementation

**Create new files in `sqldb-mysql/src/main/scala/works/iterative/sqldb/mysql/`:**

1. **`MySQLMessageCatalogueRepository.scala`**
   - Copy from PostgreSQL version
   - Update to use Magnum MySQL codecs
   - Adjust for any MySQL-specific SQL differences

2. **Migration:** `sqldb-mysql/src/main/resources/db/migration/mysql/V1__create_message_catalogue.sql`
   - Already created in `core/jvm/src/main/resources/db/migration/V1__create_message_catalogue.mysql.sql`
   - Move to `sqldb-mysql/src/main/resources/db/migration/mysql/V1__create_message_catalogue.sql`
   - **Note:** Remove `.mysql` suffix - directory path indicates database type

3. **`MySQLMessageCatalogueMigration.scala`**
   - Copy and adapt from PostgreSQL version

4. **`MySQLMessageCatalogueMigrationCLI.scala`**
   - Copy and adapt from PostgreSQL version

5. **Tests:**
   - Copy test structure from PostgreSQL
   - Update to use MySQL TestContainers
   - Create MySQL-specific test resources

### Phase 3: Update Core Abstractions

#### 3.1 Update `MessageCatalogueRepository` Trait

**Location:** `sqldb/src/main/scala/works/iterative/sqldb/MessageCatalogueRepository.scala`

**Change the layer definition to be abstract:**

```scala
trait MessageCatalogueRepository {
  def getAllForLanguage(language: Language): Task[Seq[MessageCatalogue]]
  def bulkInsert(entities: Seq[MessageCatalogueCreator]): Task[Unit]
}

object MessageCatalogueRepository {
  // Remove concrete layer implementation
  // Database-specific modules will provide their own layers
}
```

#### 3.2 Create Database-Specific Layers

**In `sqldb-postgresql` module:**
```scala
object PostgreSQLMessageCatalogueRepository {
  val layer: URLayer[PostgreSQLTransactor, MessageCatalogueRepository] =
    ZLayer.fromFunction((ts: PostgreSQLTransactor) =>
      new PostgreSQLMessageCatalogueRepositoryImpl(ts)
    )
}
```

**In `sqldb-mysql` module:**
```scala
object MySQLMessageCatalogueRepository {
  val layer: URLayer[MySQLTransactor, MessageCatalogueRepository] =
    ZLayer.fromFunction((ts: MySQLTransactor) =>
      new MySQLMessageCatalogueRepositoryImpl(ts)
    )
}
```

### Phase 4: Flyway Configuration for Consuming Applications

#### 4.1 Migration Location Configuration

**Important:** Consuming applications must configure Flyway to use the correct migration directory.

**PostgreSQL Projects:**
```scala
// Example Flyway configuration
FlywayConfig(
  locations = Seq("classpath:db/migration/postgresql")
)
```

**MySQL Projects:**
```scala
// Example Flyway configuration
FlywayConfig(
  locations = Seq("classpath:db/migration/mysql")
)
```

**Why This Matters:**
- By default, Flyway looks in `classpath:db/migration`
- Our migrations are in `db/migration/postgresql/` or `db/migration/mysql/`
- Without configuration, Flyway won't find the migrations
- Each database module packages only its own migrations

#### 4.2 Document Configuration Requirements

**Create documentation** (in README or migration guide):

```markdown
## Flyway Configuration

When using `sqldb-postgresql`, configure Flyway migration location:

```scala
import works.iterative.sqldb.FlywayMigrationService

val flywayConfig = FlywayConfig(
  locations = Seq("classpath:db/migration/postgresql"),
  // ... other config
)
```

When using `sqldb-mysql`, use:

```scala
val flywayConfig = FlywayConfig(
  locations = Seq("classpath:db/migration/mysql"),
  // ... other config
)
```
```

#### 4.3 Testing Layer Configuration

**Update testing layers to configure migration locations:**

**PostgreSQL Testing:**
```scala
// In sqldb-postgresql/testing-support/
object PostgreSQLTestingLayers {
  val flywayMigrationServiceLayer = ZLayer {
    for {
      config <- ZIO.config(FlywayMigrationServiceConfig.config)
      // Force PostgreSQL migration location
      updatedConfig = config.copy(
        locations = Seq("classpath:db/migration/postgresql")
      )
      service <- FlywayMigrationService.make(updatedConfig)
    } yield service
  }
}
```

**MySQL Testing:**
```scala
// In sqldb-mysql/testing-support/
object MySQLTestingLayers {
  val flywayMigrationServiceLayer = ZLayer {
    for {
      config <- ZIO.config(FlywayMigrationServiceConfig.config)
      // Force MySQL migration location
      updatedConfig = config.copy(
        locations = Seq("classpath:db/migration/mysql")
      )
      service <- FlywayMigrationService.make(updatedConfig)
    } yield service
  }
}
```

### Phase 5: Update Tests

#### 5.1 Update `sqldb.testing` Module

**Keep database-agnostic test utilities in `sqldb/testing-support/`**

#### 5.2 Create PostgreSQL Testing Module

**Location:** `sqldb-postgresql/testing-support/`
- Move `PostgreSQLTestingLayers.scala` here
- Move PostgreSQL-specific test utilities

#### 5.3 Create MySQL Testing Module

**Location:** `sqldb-mysql/testing-support/`
- Create `MySQLTestingLayers.scala` (mirror PostgreSQL version)
- MySQL TestContainers setup
- MySQL-specific test utilities

### Phase 5: Update Documentation

#### 5.1 Update README or Create Migration Guide

**Document:**
- Projects using PostgreSQL: depend on `sqldb-postgresql`
- Projects using MySQL: depend on `sqldb-mysql`
- How to migrate existing code that imports from `sqldb`

#### 5.2 Update Import Statements Guide

**Old imports:**
```scala
import works.iterative.sqldb.MessageCatalogueRepository
import works.iterative.sqldb.MessageCatalogueRepository.layer
```

**New imports for PostgreSQL:**
```scala
import works.iterative.sqldb.MessageCatalogueRepository // trait still in core
import works.iterative.sqldb.postgresql.PostgreSQLMessageCatalogueRepository
import works.iterative.sqldb.postgresql.PostgreSQLMessageCatalogueRepository.layer
```

**New imports for MySQL:**
```scala
import works.iterative.sqldb.MessageCatalogueRepository // trait still in core
import works.iterative.sqldb.mysql.MySQLMessageCatalogueRepository
import works.iterative.sqldb.mysql.MySQLMessageCatalogueRepository.layer
```

### Phase 6: Verification

#### 6.1 Build Verification
```bash
# Verify all modules build
mill sqldb.compile
mill sqldb-postgresql.compile
mill sqldb-mysql.compile

# Verify tests
mill sqldb-postgresql.test
mill sqldb-mysql.test
```

#### 6.2 Dependency Verification

**Check PostgreSQL module doesn't pull MySQL:**
```bash
mill show sqldb-postgresql.transitiveIvyDeps
# Should NOT include mysql-connector-j
```

**Check MySQL module doesn't pull PostgreSQL:**
```bash
mill show sqldb-mysql.transitiveIvyDeps
# Should NOT include postgresql driver
```

#### 6.3 Migration Testing

**Test that migrations work for both databases:**
```bash
# PostgreSQL
mill sqldb-postgresql.test

# MySQL
mill sqldb-mysql.test
```

## Current File Locations (Before Split)

### Source Files
- `sqldb/src/main/scala/works/iterative/sqldb/MessageCatalogueRepository.scala` → **Move to sqldb-postgresql**
- `sqldb/src/main/scala/works/iterative/sqldb/migration/MessageCatalogueMigration.scala` → **Move to sqldb-postgresql**
- `sqldb/src/main/scala/works/iterative/sqldb/migration/MessageCatalogueMigrationCLI.scala` → **Move to sqldb-postgresql**

### Migration Files
- `core/jvm/src/main/resources/db/migration/V1__create_message_catalogue.postgresql.sql`
  → **Move to** `sqldb-postgresql/src/main/resources/db/migration/postgresql/V1__create_message_catalogue.sql` (remove suffix)
- `core/jvm/src/main/resources/db/migration/V1__create_message_catalogue.mysql.sql`
  → **Move to** `sqldb-mysql/src/main/resources/db/migration/mysql/V1__create_message_catalogue.sql` (remove suffix)

### Test Files
- `sqldb/src/test/scala/works/iterative/sqldb/*` → **Move to sqldb-postgresql/src/test**
- `sqldb/src/test/resources/*` → **Move to sqldb-postgresql/src/test/resources**

## Dependencies to Check

**Magnum MySQL Support:**
- Check if `magnumMySQL` exists in IWMillDeps
- If not, might need to use `magnumZIO` + manual MySQL codecs
- Check Magnum documentation: https://github.com/AugustNagro/magnum

**Flyway MySQL:**
- `org.flywaydb:flyway-database-mysql:11.4.0` (confirmed exists)

**MySQL JDBC Driver:**
- `com.mysql:mysql-connector-j:9.1.0` (official Oracle MySQL driver)
- Alternative: `org.mariadb.jdbc:mariadb-java-client:3.3.0` (if MariaDB specific)

## Testing Strategy

### For Each Database Module

1. **Unit Tests**
   - Repository CRUD operations
   - Migration logic
   - CLI argument parsing

2. **Integration Tests**
   - TestContainers with actual database
   - Full migration workflow
   - Service layer integration

3. **Cross-Database Compatibility Tests**
   - Same test scenarios for PostgreSQL and MySQL
   - Verify both produce identical behavior
   - Test data portability

## Success Criteria

- [ ] `sqldb` module has no database-specific dependencies
- [ ] `sqldb-postgresql` module compiles and all tests pass
- [ ] `sqldb-mysql` module compiles and all tests pass
- [ ] PostgreSQL dependencies NOT in MySQL module transitiveIvyDeps
- [ ] MySQL dependencies NOT in PostgreSQL module transitiveIvyDeps
- [ ] All 68+ existing tests still passing (under new structure)
- [ ] Documentation updated for both database options
- [ ] Migration guide created for existing projects

## Risks and Mitigations

### Risk 1: Magnum MySQL Support Missing
**Mitigation:** If `magnumMySQL` doesn't exist, use `magnumZIO` with custom codecs for MySQL-specific types.

### Risk 2: Breaking Changes for Existing Projects
**Mitigation:** Create clear migration guide. Consider adding deprecation warnings in old locations.

### Risk 3: Test Duplication
**Mitigation:** Extract common test logic to `sqldb.testing` module, reuse in both PostgreSQL and MySQL tests.

### Risk 4: Flyway Configuration Complexity
**Mitigation:** Document Flyway location configuration for each database. Ensure migration paths are clear.

## Next Steps (for Implementation Session)

1. Start with Phase 1: Create module structure in `build.mill`
2. Verify modules compile (empty initially)
3. Move PostgreSQL code to `sqldb-postgresql` (Phase 2.2)
4. Create MySQL implementations (Phase 2.3)
5. Run tests to verify everything works
6. Update documentation

## Notes

- This is a **breaking change** for projects currently depending on `sqldb`
- Projects will need to update their dependencies to `sqldb-postgresql` or `sqldb-mysql`
- Consider creating a compatibility shim if needed for gradual migration
- The core `MessageCatalogueRepository` trait remains in `sqldb` (stable interface)

## Related Files

- Current PR: https://github.com/iterative-works/support/pull/5
- Tasks file: project-management/issues/IWSD-76/tasks.md
- This document: project-management/issues/IWSD-76/sqldb-split-tasks.md
