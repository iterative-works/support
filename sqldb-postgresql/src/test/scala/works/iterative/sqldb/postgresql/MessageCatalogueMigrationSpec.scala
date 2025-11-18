// PURPOSE: Tests for MessageCatalogueMigration to verify JSON to SQL migration functionality
// PURPOSE: Ensures correct parsing, conversion, and bulk insertion of messages from JSON resources

package works.iterative.sqldb.postgresql

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.core.{Language, MessageId}
import works.iterative.core.repository.MessageCatalogueRepository
import works.iterative.sqldb.testing.PostgreSQLTestingLayers.*
import works.iterative.sqldb.FlywayMigrationService
import works.iterative.sqldb.migration.MessageCatalogueMigration

object MessageCatalogueMigrationSpec extends ZIOSpecDefault:

  def spec = suite("MessageCatalogueMigrationSpec")(
    test("migrateFromJson successfully migrates 5 sample messages") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Migrate test messages
        _ <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.EN,
          "/test_messages.json"
        )
        // Verify all messages were inserted
        messages <- repository.getAllForLanguage(Language.EN)
      yield assertTrue(messages.size == 5)
    },

    test("migrateFromJson inserts messages with correct keys") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        _ <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.EN,
          "/test_messages.json"
        )
        messages <- repository.getAllForLanguage(Language.EN)
        messageKeys = messages.map(_.messageKey.value).toSet
      yield assertTrue(
        messageKeys.contains("test.message.one") &&
        messageKeys.contains("test.message.two") &&
        messageKeys.contains("test.message.three") &&
        messageKeys.contains("test.message.four") &&
        messageKeys.contains("test.message.five")
      )
    },

    test("migrateFromJson inserts messages with correct text") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        _ <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.EN,
          "/test_messages.json"
        )
        messages <- repository.getAllForLanguage(Language.EN)
        messageOne = messages.find(_.messageKey.value == "test.message.one")
      yield assertTrue(
        messageOne.isDefined &&
        messageOne.get.messageText == "First test message"
      )
    },

    test("migrateFromJson adds migration description to messages") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        _ <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.EN,
          "/test_messages.json"
        )
        messages <- repository.getAllForLanguage(Language.EN)
        message = messages.head
      yield assertTrue(
        message.description.isDefined &&
        message.description.get.contains("Migrated from /test_messages.json")
      )
    },

    test("migrateFromJson handles messages with special characters") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        _ <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.EN,
          "/test_messages.json"
        )
        messages <- repository.getAllForLanguage(Language.EN)
        messageFive = messages.find(_.messageKey.value == "test.message.five")
      yield assertTrue(
        messageFive.isDefined &&
        messageFive.get.messageText == "Fifth test message with special chars: áčďěňš"
      )
    },

    test("migrateFromJson fails with descriptive error for missing resource") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        result <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.EN,
          "/nonexistent.json"
        ).either
      yield assertTrue(
        result.isLeft &&
        result.left.exists(_.getMessage.contains("Resource not found"))
      )
    },

    test("migrateFromJson fails with descriptive error for invalid JSON") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        result <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.EN,
          "/invalid.json"
        ).either
      yield assertTrue(
        result.isLeft &&
        result.left.exists(_.getMessage.contains("Failed to parse JSON"))
      )
    }
  ).provideSomeShared[Scope](
    MessageCatalogueRepository.layer,
    flywayMigrationServiceLayer
  ) @@ sequential
