# PostgreSQL Support Libraries

This guide covers our standard PostgreSQL infrastructure components for database access and testing, providing a consistent approach to database operations in ZIO applications.

## Core Infrastructure Components

Our project provides three main infrastructure components for PostgreSQL database access:

1. **PostgreSQLDataSource** - Manages the HikariCP connection pool for PostgreSQL
2. **PostgreSQLTransactor** - Wraps a Magnum SQL Transactor for database operations
3. **PostgreSQLDatabaseSupport** - Provides combined layers and migration utilities

## PostgreSQLDataSource

The `PostgreSQLDataSource` class manages a connection pool using HikariCP:

```scala
package works.iterative.sqldb

class PostgreSQLDataSource(val dataSource: DataSource)

object PostgreSQLDataSource:
    // Initialize a HikariCP data source with configuration
    def initDataSource(config: PostgreSQLConfig): ZIO[Scope, Throwable, HikariDataSource] = ...

    // Layer that creates a DataSource from configuration
    val layer: ZLayer[Scope, Throwable, DataSource] = ZLayer:
        for
            config <- ZIO.config[PostgreSQLConfig](PostgreSQLConfig.config)
            dataSource <- initDataSource(config)
        yield dataSource

    // Layer with explicit config
    def layerWithConfig(config: PostgreSQLConfig): ZLayer[Scope, Throwable, DataSource] = ...

    // Layer that creates a managed PostgreSQLDataSource instance
    val managedLayer: ZLayer[Scope, Throwable, PostgreSQLDataSource] =
        layer >>> ZLayer.fromFunction(PostgreSQLDataSource.apply)

    // Layer that creates a managed PostgreSQLDataSource with explicit config
    def managedLayerWithConfig(config: PostgreSQLConfig): ZLayer[Scope, Throwable, PostgreSQLDataSource] = ...

    // Layer that wraps an existing DataSource
    val layerFromDataSource: ZLayer[DataSource, Nothing, PostgreSQLDataSource] = ...
```

### Usage

```scala
// Using default configuration from the environment
val program = for
  ds <- ZIO.service[PostgreSQLDataSource]
  // Use the DataSource
yield ...

program.provide(PostgreSQLDataSource.managedLayer)

// Using explicit configuration
val customConfig = PostgreSQLConfig(
  jdbcUrl = "jdbc:postgresql://localhost:5432/mydb",
  username = "user",
  password = "pass"
)

program.provide(PostgreSQLDataSource.managedLayerWithConfig(customConfig))
```

## PostgreSQLTransactor

The `PostgreSQLTransactor` creates and manages a Magnum Transactor for type-safe SQL operations:

```scala
package works.iterative.sqldb

class PostgreSQLTransactor(val transactor: Transactor)

object PostgreSQLTransactor:
    // Layer that creates a Transactor from a PostgreSQLDataSource
    val layer: ZLayer[PostgreSQLDataSource & Scope, Throwable, Transactor] =
        ZLayer.service[PostgreSQLDataSource].flatMap { env =>
            Transactor.layer(env.get[PostgreSQLDataSource].dataSource)
        }

    // Layer that creates a managed PostgreSQLTransactor instance
    val managedLayer: ZLayer[PostgreSQLDataSource & Scope, Throwable, PostgreSQLTransactor] =
        layer >>> ZLayer.fromFunction(PostgreSQLTransactor.apply)
```

### Usage with Magnum SQL

```scala
class UserService(ts: PostgreSQLTransactor):
  import com.augustnagro.magnum.*
  import com.augustnagro.magnum.magzio.*

  def findUserById(id: Long): Task[Option[User]] =
    ts.transactor.connect:
      // Type-safe Magnum SQL query
      UserRepo.repo.findById(id)

  def createUser(user: UserDTO): Task[User] =
    ts.transactor.transact:
      UserRepo.repo.insertReturning(user)

// Create a service that depends on the transactor
val userServiceLayer = ZLayer.fromFunction(UserService(_))

val fullServiceLayer = PostgreSQLDataSource.managedLayer >>>
  PostgreSQLTransactor.managedLayer >>>
  userServiceLayer
```

## PostgreSQLDatabaseSupport

The `PostgreSQLDatabaseSupport` provides combined layers and migration utilities:

```scala
package works.iterative.sqldb

object PostgreSQLDatabaseSupport:
    // Base infrastructure type including data source and transactor
    type BaseDatabaseInfrastructure = PostgreSQLDataSource & PostgreSQLTransactor

    // Creates a ZLayer with the base database infrastructure
    val layer: ZLayer[Scope, Throwable, BaseDatabaseInfrastructure] = ...

    // Creates a ZLayer with both infrastructure and migration service
    def layerWithMigrations(additionalLocations: List[String] = List.empty)
      : ZLayer[Scope, Throwable, BaseDatabaseInfrastructure & FlywayMigrationService] = ...

    // Runs migrations directly
    def migrate(additionalLocations: List[String] = List.empty): ZIO[Scope, Throwable, Unit] = ...
```

### Usage

```scala
// Get complete database infrastructure with migrations
val program = for
  _ <- ZIO.log("Database ready with migrations applied")
  service <- ZIO.service[MyService]
  result <- service.doSomething()
yield result

// Provide the infrastructure with migrations that run automatically
program.provide(
  PostgreSQLDatabaseSupport.layerWithMigrations() >>>
  myServiceLayer
)
```

## Database Migrations with Flyway

We use Flyway for database migrations, providing a structured way to evolve the database schema:

```scala
package works.iterative.sqldb

trait FlywayMigrationService:
    def migrate(): Task[MigrateResult]  // Apply pending migrations
    def clean(): Task[Unit]             // Clean/reset the database
    def validate(): Task[Unit]          // Validate migrations
    def info(): Task[Unit]              // Print migration info

case class FlywayConfig(
    locations: List[String] = List(FlywayConfig.DefaultLocation),
    cleanDisabled: Boolean = true
)

object FlywayConfig:
    val DefaultLocation = "classpath:db/migration"
    val default: FlywayConfig = FlywayConfig()
```

### Migration File Organization

Flyway migrations should be placed in the `src/main/resources/db/migration` directory with file names following the format:

- `V{version}__{description}.sql` - For versioned migrations (e.g., `V1__create_users_table.sql`)
- `R__{description}.sql` - For repeatable migrations (e.g., `R__views.sql`)

Versioned migrations run in order based on the version number, while repeatable migrations run whenever their content changes.

### Using Migrations

```scala
// Get the migration service
val migrateProgram = for
  migrationService <- ZIO.service[FlywayMigrationService]
  _ <- migrationService.migrate()
  _ <- ZIO.log("Migrations applied successfully")
yield ()

migrateProgram.provide(
  PostgreSQLDataSource.managedLayer >>>
  FlywayMigrationService.layer
)
```

## Testing Infrastructure

Our `iw-support-sqldb-testing` module provides standardized components for testing with PostgreSQL databases.

### PostgreSQLTestingLayers

The `PostgreSQLTestingLayers` object provides ZLayers for TestContainers and database infrastructure:

```scala
package works.iterative.sqldb.testing

object PostgreSQLTestingLayers:
    // Container for PostgreSQL test database
    val postgresContainer: ZLayer[Scope, Throwable, PostgreSQLContainer] = ...

    // DataSource connected to test container
    val dataSourceLayer: ZLayer[Scope, Throwable, DataSource] = ...

    // PostgreSQLDataSource layer
    val postgreSQLDataSourceLayer: ZLayer[Scope, Throwable, PostgreSQLDataSource] = ...

    // Transactor layer for database operations
    val transactorLayer: ZLayer[Scope, Throwable, Transactor] = ...

    // Combined layer with both DataSource and Transactor
    val postgreSQLTransactorLayer: ZLayer[Scope, Throwable, PostgreSQLDataSource & PostgreSQLTransactor] = ...

    // Test Flyway config that allows cleaning the database
    val testFlywayConfig = FlywayConfig(
        locations = FlywayConfig.DefaultLocation :: Nil,
        cleanDisabled = false
    )

    // Full layer with DataSource, Transactor and FlywayMigrationService
    val flywayMigrationServiceLayer: ZLayer[
        Scope,
        Throwable,
        PostgreSQLDataSource & PostgreSQLTransactor & FlywayMigrationService
    ] = ...
```

### MigrateAspects

The `MigrateAspects` object provides test aspects for database setup and teardown:

```scala
package works.iterative.sqldb.testing

object MigrateAspects:
    // Set up fresh database schema for tests
    val setupDbSchema = ZIO.scoped {
        for
            migrationService <- ZIO.service[FlywayMigrationService]
            // Clean the database to ensure a fresh state
            _ <- migrationService.clean()
            // Run migrations to set up the schema
            result <- migrationService.migrate()
        yield ()
    }

    // Test aspect that runs migrations before tests
    val migrate = TestAspect.before(setupDbSchema)
```

### Writing Tests with PostgreSQL

```scala
import zio.*
import zio.test.*
import works.iterative.sqldb.testing.PostgreSQLTestingLayers.*
import works.iterative.sqldb.testing.MigrateAspects.*

object UserServiceSpec extends ZIOSpecDefault:
    // Define the service layer
    val serviceLayer = myServiceLayer

    def spec = {
        suite("UserService")(
            test("should create a user") {
                for
                    service <- ZIO.service[MyService]
                    result <- service.createUser(...)
                yield assertTrue(...)
            }
        // Apply aspects for all tests
        ) @@ sequential @@ migrate
    }.provideSomeShared[Scope](
        // Provide infrastructure layers
        flywayMigrationServiceLayer,
        serviceLayer
    )
```

## Best Practices

1. **Use managedLayer Variants** - Always use the managed layer variants like `PostgreSQLDataSource.managedLayer` rather than accessing the lower-level layers directly.

2. **Connection Management** - Let ZIO's scoping manage the lifecycle of connections to prevent leaks.

3. **Transaction Boundaries** - Use `transactor.connect` for read-only operations and `transactor.transact` for write operations that need transactions.

4. **Clean State for Tests** - Always use the `migrate` aspect in tests to ensure each test starts with a clean database state.

5. **Sequential Test Execution** - Use the `sequential` aspect when multiple tests access the same database to prevent test interference.

6. **Proper Migration Versioning** - Follow proper version numbering for migrations (V1, V2, etc.) and use descriptive names.

7. **Configuration Management** - Use ZIO Config for production and explicit configuration objects for tests.
