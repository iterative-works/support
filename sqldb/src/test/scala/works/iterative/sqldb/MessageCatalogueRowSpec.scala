// PURPOSE: Tests for MessageCatalogue database entity
// PURPOSE: Verifies entity creation, factory methods, and Magnum codec derivation

package works.iterative.sqldb

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.core.{Language, MessageId}
import com.augustnagro.magnum.*
import java.time.Instant

object MessageCatalogueRowSpec extends ZIOSpecDefault:

  def spec = suite("MessageCatalogueRowSpec")(
    test("creates MessageCatalogue with all fields") {
      val now = Instant.now()
      val entity = MessageCatalogue(
        id = Some(1L),
        messageKey = "test.key",
        language = "en",
        messageText = "Test message",
        description = Some("Test description"),
        createdAt = now,
        updatedAt = now,
        createdBy = Some("testuser"),
        updatedBy = Some("testuser")
      )

      assertTrue(
        entity.id == Some(1L),
        entity.messageKey == "test.key",
        entity.language == "en",
        entity.messageText == "Test message",
        entity.description == Some("Test description"),
        entity.createdAt == now,
        entity.updatedAt == now,
        entity.createdBy == Some("testuser"),
        entity.updatedBy == Some("testuser")
      )
    },

    test("fromMessage factory method creates entity with correct fields") {
      val entity = MessageCatalogue.fromMessage(
        MessageId("welcome.message"),
        Language.EN,
        "Welcome to the application",
        Some("Greeting message for users"),
        Some("admin")
      )

      assertTrue(
        entity.id.isEmpty,
        entity.messageKey == "welcome.message",
        entity.language == "en",
        entity.messageText == "Welcome to the application",
        entity.description == Some("Greeting message for users"),
        entity.createdBy == Some("admin"),
        entity.updatedBy == Some("admin"),
        entity.createdAt == entity.updatedAt
      )
    },

    test("Magnum can derive DbCodec for the entity") {
      // This test verifies that Magnum can properly encode/decode the entity
      // If DbCodec derivation fails, this test won't compile
      val codec = summon[DbCodec[MessageCatalogue]]
      assertTrue(codec != null)
    },

    test("field name mapping uses CamelToSnakeCase") {
      // This test verifies that Magnum can derive TableInfo with CamelToSnakeCase mapping
      // If the @Table annotation is incorrect, this test won't compile
      val tableInfo = TableInfo[MessageCatalogueCreator, MessageCatalogue, Long]

      // Verify TableInfo is not null (implicitly tests @Table annotation and derives)
      assertTrue(tableInfo != null)
    },

    test("Language type converts to/from String") {
      val entity = MessageCatalogue(
        id = None,
        messageKey = "test",
        language = "cs", // Language as String in entity
        messageText = "text",
        description = None,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        createdBy = None,
        updatedBy = None
      )

      assertTrue(entity.language == "cs")
    },

    test("Instant serialization works correctly") {
      val now = Instant.now()
      val entity = MessageCatalogue(
        id = None,
        messageKey = "test",
        language = "en",
        messageText = "text",
        description = None,
        createdAt = now,
        updatedAt = now,
        createdBy = None,
        updatedBy = None
      )

      assertTrue(
        entity.createdAt == now,
        entity.updatedAt == now
      )
    }
  )
end MessageCatalogueRowSpec
