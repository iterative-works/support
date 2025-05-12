package works.iterative.sqldb
package testing

import zio.*
import zio.test.TestAspect

object MigrateAspects:
    // Use Flyway to manage schema setup and teardown for tests
    val setupDbSchema = ZIO.scoped {
        for
            // Get the migration service
            migrationService <- ZIO.service[FlywayMigrationService]
            // First clean the database to ensure a fresh state
            _ <- migrationService.clean()
            // Then run migrations to set up the schema
            result <- migrationService.migrate()
            _ <- ZIO.log(s"Migration complete: ${result.migrationsExecuted} migrations executed")
        yield ()
    }

    // Run migration and cleanup before each test
    val migrate = TestAspect.before(setupDbSchema)

    // Run migration and cleanup before a suite
    val migrateOnce = TestAspect.beforeAll(setupDbSchema)
end MigrateAspects
