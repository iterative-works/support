// PURPOSE: Tests for real messages.json file migration to verify production data compatibility
// PURPOSE: Ensures migration handles actual project message files with all edge cases

package works.iterative.sqldb.postgresql
import works.iterative.sqldb.FlywayMigrationService

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.core.{Language, MessageId}
import works.iterative.core.repository.MessageCatalogueRepository
import works.iterative.core.service.impl.SqlMessageCatalogueService
import works.iterative.sqldb.postgresql.testing.PostgreSQLTestingLayers.*
import works.iterative.sqldb.postgresql.migration.MessageCatalogueMigration

object RealMessageMigrationSpec extends ZIOSpecDefault:

  def spec = suite("RealMessageMigrationSpec")(
    test("migrates real CS messages file successfully") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Migrate real CS messages
        _ <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.CS,
          "/messages_cs_real.json"
        )
        // Verify all messages were inserted
        messages <- repository.getAllForLanguage(Language.CS)
      yield assertTrue(messages.size == 11) // 11 messages in the real file
    },

    test("migrated CS messages have correct keys from real file") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        _ <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.CS,
          "/messages_cs_real.json"
        )
        messages <- repository.getAllForLanguage(Language.CS)
        messageKeys = messages.map(_.messageKey.value).toSet
      yield assertTrue(
        messageKeys.contains("dokument.loading") &&
        messageKeys.contains("dokument.nazev") &&
        messageKeys.contains("jazyk.cs") &&
        messageKeys.contains("error.server")
      )
    },

    test("migrated CS messages contain special Czech characters") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        _ <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.CS,
          "/messages_cs_real.json"
        )
        messages <- repository.getAllForLanguage(Language.CS)
        czechMessage = messages.find(_.messageKey.value == "jazyk.cs")
      yield assertTrue(
        czechMessage.isDefined &&
        czechMessage.get.messageText == "čeština" // Contains special char č
      )
    },

    test("SqlMessageCatalogue can retrieve all migrated real messages") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        _ <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.CS,
          "/messages_cs_real.json"
        )
        // Create service using migrated messages
        service <- SqlMessageCatalogueService.make(
          repository,
          Seq(Language.CS),
          Language.CS
        )
        catalogue <- service.forLanguage(Language.CS)
        // Verify we can retrieve messages
        loading = catalogue.get(MessageId("dokument.loading"))
        czech = catalogue.get(MessageId("jazyk.cs"))
      yield assertTrue(
        loading.isDefined &&
        loading.get.contains("Nahrávám dokument") &&
        czech.isDefined &&
        czech.get == "čeština"
      )
    },

    test("migrated messages contain formatting placeholders") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        _ <- MessageCatalogueMigration.migrateFromJson(
          repository,
          Language.CS,
          "/messages_cs_real.json"
        )
        messages <- repository.getAllForLanguage(Language.CS)
        loadingMessage = messages.find(_.messageKey.value == "dokument.loading")
      yield assertTrue(
        loadingMessage.isDefined &&
        loadingMessage.get.messageText.contains("%s") // Contains placeholder
      )
    }
  ).provideSomeShared[Scope](
    PostgreSQLMessageCatalogueRepository.layer,
    flywayMigrationServiceLayer
  ) @@ sequential

end RealMessageMigrationSpec
