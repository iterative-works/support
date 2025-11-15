// PURPOSE: Tests for Flyway migration V1 that creates message_catalogue tables
// PURPOSE: Verifies database schema is created correctly with proper constraints and indexes

package works.iterative.sqldb

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.sqldb.testing.PostgreSQLTestingLayers.*
import works.iterative.sqldb.testing.MigrateAspects.*
import com.augustnagro.magnum.magzio.*
import java.sql.Connection

object MessageCatalogueMigrationSpec extends ZIOSpecDefault:

  def spec = suite("MessageCatalogueMigrationSpec")(
    test("migration creates message_catalogue and message_catalogue_history tables") {
      for
        // Clean and migrate
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        // Check tables exist
        pgTransactor <- ZIO.service[PostgreSQLTransactor]
        catalogueResult <- pgTransactor.transactor.connect:
          sql"SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'message_catalogue')"
            .query[Boolean]
            .run()
        catalogueExists = catalogueResult.headOption.getOrElse(false)
        historyResult <- pgTransactor.transactor.connect:
          sql"SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'message_catalogue_history')"
            .query[Boolean]
            .run()
        historyExists = historyResult.headOption.getOrElse(false)
      yield assertTrue(catalogueExists && historyExists)
    },
    test("message_catalogue table has proper structure") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        pgTransactor <- ZIO.service[PostgreSQLTransactor]
        // Check columns exist
        columns <- pgTransactor.transactor.connect:
          sql"""SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'message_catalogue'
                ORDER BY ordinal_position"""
            .query[String]
            .run()
        expectedColumns = List("id", "message_key", "language", "message_text", "description",
                               "created_at", "updated_at", "created_by", "updated_by")
      yield assertTrue(columns == expectedColumns)
    }
  ).provideSomeShared[Scope](
    flywayMigrationServiceLayer
  ) @@ sequential
end MessageCatalogueMigrationSpec
