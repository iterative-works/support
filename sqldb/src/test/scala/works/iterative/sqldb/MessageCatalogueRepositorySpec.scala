// PURPOSE: Unit tests for MessageCatalogueRepository interface and basic functionality
// PURPOSE: Verifies repository operations including security (SQL injection) and data integrity

package works.iterative.sqldb

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.core.Language
import works.iterative.core.repository.MessageCatalogueRepository
import works.iterative.sqldb.testing.PostgreSQLTestingLayers.*
import com.augustnagro.magnum.magzio.*

object MessageCatalogueRepositorySpec extends ZIOSpecDefault:

  def spec = suite("MessageCatalogueRepositorySpec")(
    test("getAllForLanguage returns empty sequence for unknown language") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Query for language that has no messages
        messages <- repository.getAllForLanguage(Language.DE)
      yield assertTrue(messages.isEmpty)
    },

    test("bulkInsert inserts multiple entities") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Create test entities
        entities = Seq(
          MessageCatalogue.fromMessage(
            works.iterative.core.MessageId("test.key1"),
            Language.EN,
            "Test message 1",
            Some("Description 1"),
            Some("testuser")
          ).toDomain,
          MessageCatalogue.fromMessage(
            works.iterative.core.MessageId("test.key2"),
            Language.EN,
            "Test message 2",
            Some("Description 2"),
            Some("testuser")
          ).toDomain,
          MessageCatalogue.fromMessage(
            works.iterative.core.MessageId("test.key3"),
            Language.EN,
            "Test message 3",
            None,
            Some("testuser")
          ).toDomain
        )
        _ <- repository.bulkInsert(entities)
        // Verify messages were inserted
        messages <- repository.getAllForLanguage(Language.EN)
      yield assertTrue(messages.size == 3)
    },

    test("getAllForLanguage handles SQL injection attempt safely") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Insert some test data first
        entities = Seq(
          MessageCatalogue.fromMessage(
            works.iterative.core.MessageId("test.injection"),
            Language.EN,
            "Test message",
            Some("Description"),
            Some("testuser")
          ).toDomain
        )
        _ <- repository.bulkInsert(entities)
        // Attempt SQL injection - this should be safely parameterized
        // We create a malicious "language" value that would drop the table if not parameterized
        maliciousLanguage = "en'; DROP TABLE message_catalogue; --"
        // This should return empty (no match) rather than executing the DROP
        result <- repository.getAllForLanguage(Language.unsafe(maliciousLanguage))
        // Verify table still exists by querying it
        messages <- repository.getAllForLanguage(Language.EN)
      yield assertTrue(result.isEmpty && messages.nonEmpty)
    },

    test("bulkInsert handles duplicate message_key for same language with constraint violation") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Insert first batch
        entities1 = Seq(
          MessageCatalogue.fromMessage(
            works.iterative.core.MessageId("duplicate.key"),
            Language.EN,
            "First message",
            None,
            Some("testuser")
          ).toDomain
        )
        _ <- repository.bulkInsert(entities1)
        // Attempt to insert duplicate key for same language
        entities2 = Seq(
          MessageCatalogue.fromMessage(
            works.iterative.core.MessageId("duplicate.key"),
            Language.EN,
            "Second message",
            None,
            Some("testuser")
          ).toDomain
        )
        // This should fail with constraint violation
        result <- repository.bulkInsert(entities2).either
      yield assertTrue(result.isLeft)
    },

    test("bulkInsert handles large dataset (1000+ entities)") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Create 1000 entities
        entities = (1 to 1000).map { i =>
          MessageCatalogue.fromMessage(
            works.iterative.core.MessageId(s"large.dataset.key.$i"),
            Language.EN,
            s"Message $i",
            Some(s"Description $i"),
            Some("testuser")
          ).toDomain
        }
        _ <- repository.bulkInsert(entities)
        // Verify all were inserted
        messages <- repository.getAllForLanguage(Language.EN)
      yield assertTrue(messages.size == 1000)
    },

    test("getAllForLanguage returns only messages for requested language") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[MessageCatalogueRepository]
        // Insert messages for different languages
        entitiesEN = Seq(
          MessageCatalogue.fromMessage(
            works.iterative.core.MessageId("test.en.key1"),
            Language.EN,
            "English message 1",
            None,
            Some("testuser")
          ).toDomain,
          MessageCatalogue.fromMessage(
            works.iterative.core.MessageId("test.en.key2"),
            Language.EN,
            "English message 2",
            None,
            Some("testuser")
          ).toDomain
        )
        entitiesCS = Seq(
          MessageCatalogue.fromMessage(
            works.iterative.core.MessageId("test.cs.key1"),
            Language.CS,
            "Czech message 1",
            None,
            Some("testuser")
          ).toDomain
        )
        _ <- repository.bulkInsert(entitiesEN)
        _ <- repository.bulkInsert(entitiesCS)
        // Get only EN messages
        messagesEN <- repository.getAllForLanguage(Language.EN)
        // Get only CS messages
        messagesCS <- repository.getAllForLanguage(Language.CS)
      yield assertTrue(
        messagesEN.size == 2 &&
        messagesCS.size == 1 &&
        messagesEN.forall(_.language == Language.EN) &&
        messagesCS.forall(_.language == Language.CS)
      )
    }
  ).provideSomeShared[Scope](
    MessageCatalogueRepository.layer,
    flywayMigrationServiceLayer
  ) @@ sequential
end MessageCatalogueRepositorySpec
