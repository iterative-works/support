// PURPOSE: Tests for message_catalogue audit trigger functionality
// PURPOSE: Verifies that history entries are created only when message_text changes

package works.iterative.sqldb.postgresql

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.sqldb.testing.PostgreSQLTestingLayers.*
import com.augustnagro.magnum.magzio.*

object MessageCatalogueAuditTriggerSpec extends ZIOSpecDefault:

  def spec = suite("MessageCatalogueAuditTriggerSpec")(
    test("inserts message, updates message_text, verifies history entry created") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        pgTransactor <- ZIO.service[PostgreSQLTransactor]
        // Insert a message
        insertResult <- pgTransactor.transactor.transact:
          sql"""
            INSERT INTO message_catalogue (message_key, language, message_text, description, created_by, updated_by)
            VALUES ('test.key', 'en', 'Original text', 'Test description', 'testuser', 'testuser')
            RETURNING id
          """.query[Long].run()
        insertedId = insertResult.head
        // Update the message_text
        _ <- pgTransactor.transactor.transact:
          sql"""
            UPDATE message_catalogue
            SET message_text = 'Updated text', updated_by = 'updater'
            WHERE id = $insertedId
          """.update.run()
        // Check that history entry was created
        historyResult <- pgTransactor.transactor.connect:
          sql"""
            SELECT COUNT(*) FROM message_catalogue_history
            WHERE message_catalogue_id = $insertedId
            AND old_message_text = 'Original text'
            AND new_message_text = 'Updated text'
            AND changed_by = 'updater'
          """.query[Long].run()
        historyCount = historyResult.head
      yield assertTrue(historyCount == 1)
    },

    test("updates message but keeps same message_text, verifies NO history entry") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        pgTransactor <- ZIO.service[PostgreSQLTransactor]
        // Insert a message
        insertResult <- pgTransactor.transactor.transact:
          sql"""
            INSERT INTO message_catalogue (message_key, language, message_text, description, created_by, updated_by)
            VALUES ('test.key2', 'en', 'Same text', 'Test description', 'testuser', 'testuser')
            RETURNING id
          """.query[Long].run()
        insertedId = insertResult.head
        // Update description but NOT message_text
        _ <- pgTransactor.transactor.transact:
          sql"""
            UPDATE message_catalogue
            SET description = 'Updated description', updated_by = 'updater'
            WHERE id = $insertedId
          """.update.run()
        // Check that NO history entry was created
        historyResult <- pgTransactor.transactor.connect:
          sql"""
            SELECT COUNT(*) FROM message_catalogue_history
            WHERE message_catalogue_id = $insertedId
          """.query[Long].run()
        historyCount = historyResult.head
      yield assertTrue(historyCount == 0)
    },

    test("updates other fields (description), verifies NO history entry") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        pgTransactor <- ZIO.service[PostgreSQLTransactor]
        // Insert a message
        insertResult <- pgTransactor.transactor.transact:
          sql"""
            INSERT INTO message_catalogue (message_key, language, message_text, description, created_by, updated_by)
            VALUES ('test.key3', 'en', 'Fixed text', 'Original description', 'testuser', 'testuser')
            RETURNING id
          """.query[Long].run()
        insertedId = insertResult.head
        // Update only description
        _ <- pgTransactor.transactor.transact:
          sql"""
            UPDATE message_catalogue
            SET description = 'New description'
            WHERE id = $insertedId
          """.update.run()
        // Verify no history entry
        historyResult <- pgTransactor.transactor.connect:
          sql"""
            SELECT COUNT(*) FROM message_catalogue_history
            WHERE message_catalogue_id = $insertedId
          """.query[Long].run()
        historyCount = historyResult.head
      yield assertTrue(historyCount == 0)
    },

    test("verifies changed_by field in history matches updated_by from update") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        pgTransactor <- ZIO.service[PostgreSQLTransactor]
        // Insert a message
        insertResult <- pgTransactor.transactor.transact:
          sql"""
            INSERT INTO message_catalogue (message_key, language, message_text, created_by, updated_by)
            VALUES ('test.key4', 'en', 'Text version 1', 'creator', 'creator')
            RETURNING id
          """.query[Long].run()
        insertedId = insertResult.head
        // Update message_text with specific updated_by
        _ <- pgTransactor.transactor.transact:
          sql"""
            UPDATE message_catalogue
            SET message_text = 'Text version 2', updated_by = 'specific_user'
            WHERE id = $insertedId
          """.update.run()
        // Verify changed_by in history matches updated_by
        changedByResult <- pgTransactor.transactor.connect:
          sql"""
            SELECT changed_by FROM message_catalogue_history
            WHERE message_catalogue_id = $insertedId
          """.query[Option[String]].run()
        changedBy = changedByResult.head
      yield assertTrue(changedBy == Some("specific_user"))
    },

    test("multiple updates create multiple history entries") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        pgTransactor <- ZIO.service[PostgreSQLTransactor]
        // Insert a message
        insertResult <- pgTransactor.transactor.transact:
          sql"""
            INSERT INTO message_catalogue (message_key, language, message_text, created_by, updated_by)
            VALUES ('test.key5', 'en', 'Version 1', 'testuser', 'testuser')
            RETURNING id
          """.query[Long].run()
        insertedId = insertResult.head
        // First update
        _ <- pgTransactor.transactor.transact:
          sql"""
            UPDATE message_catalogue
            SET message_text = 'Version 2', updated_by = 'user1'
            WHERE id = $insertedId
          """.update.run()
        // Second update
        _ <- pgTransactor.transactor.transact:
          sql"""
            UPDATE message_catalogue
            SET message_text = 'Version 3', updated_by = 'user2'
            WHERE id = $insertedId
          """.update.run()
        // Verify two history entries exist
        historyResult <- pgTransactor.transactor.connect:
          sql"""
            SELECT COUNT(*) FROM message_catalogue_history
            WHERE message_catalogue_id = $insertedId
          """.query[Long].run()
        historyCount = historyResult.head
      yield assertTrue(historyCount == 2)
    },

    test("updated_at timestamp automatically updates on any UPDATE") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        pgTransactor <- ZIO.service[PostgreSQLTransactor]

        // Insert a message
        insertResult <- pgTransactor.transactor.transact:
          sql"""
            INSERT INTO message_catalogue (message_key, language, message_text, created_by, updated_by)
            VALUES ('test.updated_at', 'en', 'Original text', 'test_user', 'test_user')
            RETURNING id, updated_at
          """.query[(Long, java.time.OffsetDateTime)].run()
        (insertedId, originalUpdatedAt) = insertResult.head

        // Update description (not message_text, to avoid triggering audit)
        _ <- pgTransactor.transactor.transact:
          sql"""
            UPDATE message_catalogue
            SET description = 'New description', updated_by = 'test_user'
            WHERE id = $insertedId
          """.update.run()

        // Verify updated_at changed
        newUpdatedAtResult <- pgTransactor.transactor.connect:
          sql"""
            SELECT updated_at FROM message_catalogue
            WHERE id = $insertedId
          """.query[java.time.OffsetDateTime].run()
        newUpdatedAt = newUpdatedAtResult.head
      yield assertTrue(newUpdatedAt.isAfter(originalUpdatedAt))
    }
  ).provideSomeShared[Scope](
    flywayMigrationServiceLayer
  ) @@ sequential
end MessageCatalogueAuditTriggerSpec
