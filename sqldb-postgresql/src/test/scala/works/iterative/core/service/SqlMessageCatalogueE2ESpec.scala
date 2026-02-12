// PURPOSE: End-to-end integration tests for SQL message catalogue complete workflow
// PURPOSE: Validates full layer stack from database through service to message retrieval

package works.iterative.core.service

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.core.{Language, MessageId}
import works.iterative.sqldb.{MessageCatalogueData, MessageCatalogueRepository, SqlMessageCatalogueService}
import works.iterative.sqldb.FlywayMigrationService
import works.iterative.sqldb.postgresql.PostgreSQLMessageCatalogueRepository
import works.iterative.sqldb.postgresql.testing.PostgreSQLTestingLayers.*
import java.time.Instant

object SqlMessageCatalogueE2ESpec extends ZIOSpecDefault:

  def spec = suite("SqlMessageCatalogueE2ESpec")(
    test("creates full layer stack: Database >>> Transactor >>> Repository >>> Service") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        // Verify we can get all services
        repository <- ZIO.service[MessageCatalogueRepository]
        // Insert some test messages
        entities = Seq(
          MessageCatalogueData(
            MessageId("test.message"),
            Language.EN,
            "Test Message",
            Some("Test description"),
            Instant.now(),
            Instant.now(),
            Some("testuser"),
            Some("testuser")
          )
        )
        _ <- repository.bulkInsert(entities)
        // Create service - it will pre-load messages
        service <- SqlMessageCatalogueService.make(repository, Seq(Language.EN), Language.EN)
        // Get message catalogue and verify message
        catalogue <- service.forLanguage(Language.EN)
        message = catalogue.get(MessageId("test.message"))
      yield assertTrue(message.contains("Test Message"))
    },

    test("pre-loads messages at startup") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Insert test messages before service creation
        entities = Seq(
          MessageCatalogueData(
            MessageId("startup.message1"),
            Language.EN,
            "Startup Message 1",
            None,
            Instant.now(),
            Instant.now(),
            Some("testuser"),
            Some("testuser")
          ),
          MessageCatalogueData(
            MessageId("startup.message2"),
            Language.EN,
            "Startup Message 2",
            None,
            Instant.now(),
            Instant.now(),
            Some("testuser"),
            Some("testuser")
          )
        )
        _ <- repository.bulkInsert(entities)
        // Service should pre-load these messages at creation
        service <- SqlMessageCatalogueService.make(repository, Seq(Language.EN), Language.EN)
        catalogue <- service.forLanguage(Language.EN)
        message1 = catalogue.get(MessageId("startup.message1"))
        message2 = catalogue.get(MessageId("startup.message2"))
      yield assertTrue(
        message1.contains("Startup Message 1") &&
        message2.contains("Startup Message 2")
      )
    },

    test("reload updates cache atomically after database update") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Create service with no messages initially
        service <- SqlMessageCatalogueService.make(repository, Seq(Language.EN), Language.EN)
        // Initially no messages
        catalogue1 <- service.forLanguage(Language.EN)
        message1 = catalogue1.get(MessageId("reload.message"))
        // Add message to database
        entities = Seq(
          MessageCatalogueData(
            MessageId("reload.message"),
            Language.EN,
            "Reloaded Message",
            None,
            Instant.now(),
            Instant.now(),
            Some("testuser"),
            Some("testuser")
          )
        )
        _ <- repository.bulkInsert(entities)
        // Reload should pick up new message
        _ <- service.reload(Some(Language.EN))
        catalogue2 <- service.forLanguage(Language.EN)
        message2 = catalogue2.get(MessageId("reload.message"))
      yield assertTrue(
        message1.isEmpty &&
        message2.contains("Reloaded Message")
      )
    },

    test("concurrent access to message catalogue is thread-safe") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Insert test messages
        entities = (1 to 100).map { i =>
          MessageCatalogueData(
            MessageId(s"concurrent.message.$i"),
            Language.EN,
            s"Concurrent Message $i",
            None,
            Instant.now(),
            Instant.now(),
            Some("testuser"),
            Some("testuser")
          )
        }
        _ <- repository.bulkInsert(entities)
        service <- SqlMessageCatalogueService.make(repository, Seq(Language.EN), Language.EN)
        // Access messages concurrently from 10 threads
        results <- ZIO.foreachPar((1 to 10).toList) { _ =>
          for
            catalogue <- service.forLanguage(Language.EN)
            messages = (1 to 100).map(i => catalogue.get(MessageId(s"concurrent.message.$i")))
          yield messages.flatten.size
        }
      yield assertTrue(results.forall(_ == 100))
    },

    test("fallback chain behavior works correctly") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Insert messages with fallback keys
        entities = Seq(
          MessageCatalogueData(
            MessageId("fallback.default"),
            Language.EN,
            "Default Message",
            None,
            Instant.now(),
            Instant.now(),
            Some("testuser"),
            Some("testuser")
          )
        )
        _ <- repository.bulkInsert(entities)
        service <- SqlMessageCatalogueService.make(repository, Seq(Language.EN), Language.EN)
        catalogue <- service.forLanguage(Language.EN)
        // Test fallback: missing key falls back to default
        message = catalogue.apply(MessageId("fallback.missing"), MessageId("fallback.default"))
      yield assertTrue(message == "Default Message")
    },

    test("nested message catalogue with prefixes works correctly") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Insert messages with prefixed keys
        entities = Seq(
          MessageCatalogueData(
            MessageId("module.feature.title"),
            Language.EN,
            "Feature Title",
            None,
            Instant.now(),
            Instant.now(),
            Some("testuser"),
            Some("testuser")
          ),
          MessageCatalogueData(
            MessageId("title"),
            Language.EN,
            "Generic Title",
            None,
            Instant.now(),
            Instant.now(),
            Some("testuser"),
            Some("testuser")
          )
        )
        _ <- repository.bulkInsert(entities)
        service <- SqlMessageCatalogueService.make(repository, Seq(Language.EN), Language.EN)
        catalogue <- service.forLanguage(Language.EN)
        // Create nested catalogue with prefixes
        nested = catalogue.nested("module.feature")
        // Should find prefixed version first
        message1 = nested.get(MessageId("title"))
        // Should fall back to unprefixed version
        nested2 = catalogue.nested("other.module")
        message2 = nested2.get(MessageId("title"))
      yield assertTrue(
        message1.contains("Feature Title") &&
        message2.contains("Generic Title")
      )
    },

    test("migrates JSON messages, creates service, retrieves messages") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Simulate migration from JSON
        entities = Seq(
          MessageCatalogueData(
            MessageId("json.migrated.welcome"),
            Language.CS,
            "Vítejte",
            Some("Migrated from JSON"),
            Instant.now(),
            Instant.now(),
            Some("migration-tool"),
            Some("migration-tool")
          ),
          MessageCatalogueData(
            MessageId("json.migrated.goodbye"),
            Language.CS,
            "Na shledanou",
            Some("Migrated from JSON"),
            Instant.now(),
            Instant.now(),
            Some("migration-tool"),
            Some("migration-tool")
          )
        )
        _ <- repository.bulkInsert(entities)
        // Create service (simulating fresh start after migration)
        service <- SqlMessageCatalogueService.make(
          repository,
          Seq(Language.CS),
          Language.CS
        )
        catalogue <- service.forLanguage(Language.CS)
        welcome = catalogue.get(MessageId("json.migrated.welcome"))
        goodbye = catalogue.get(MessageId("json.migrated.goodbye"))
      yield assertTrue(
        welcome.contains("Vítejte") &&
        goodbye.contains("Na shledanou")
      )
    }
  ).provideSomeShared[Scope](
    flywayMigrationServiceLayer,
    PostgreSQLMessageCatalogueRepository.layer
  ) @@ sequential

end SqlMessageCatalogueE2ESpec
