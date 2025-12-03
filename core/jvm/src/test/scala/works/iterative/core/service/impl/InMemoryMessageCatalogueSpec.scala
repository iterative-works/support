// PURPOSE: Test suite for InMemoryMessageCatalogue validating synchronous message lookup
// PURPOSE: Verifies message retrieval, template formatting, and error handling

package works.iterative.core
package service.impl

import zio.test.*
import works.iterative.core.{Language, MessageId, UserMessage}

object InMemoryMessageCatalogueSpec extends ZIOSpecDefault:

  def spec = suite("InMemoryMessageCatalogue")(
    test("should return message for existing key") {
      val messages = Map("test.key" -> "Test Message")
      val catalogue = new InMemoryMessageCatalogue(Language.EN, messages)

      assertTrue(catalogue.get(MessageId("test.key")) == Some("Test Message"))
    },

    test("should return None for missing key") {
      val messages = Map("test.key" -> "Test Message")
      val catalogue = new InMemoryMessageCatalogue(Language.EN, messages)

      assertTrue(catalogue.get(MessageId("missing")) == None)
    },

    test("should format message with arguments") {
      val messages = Map("greet" -> "Hello %s")
      val catalogue = new InMemoryMessageCatalogue(Language.EN, messages)

      assertTrue(catalogue.get(UserMessage("greet", "John")) == Some("Hello John"))
    },

    test("should handle formatting errors with error message") {
      val messages = Map("bad.format" -> "Hello %s %s")
      val catalogue = new InMemoryMessageCatalogue(Language.EN, messages)

      val result = catalogue.get(UserMessage("bad.format", "John"))
      assertTrue(
        result.exists(_.startsWith("error formatting [bad.format]: 'Hello %s %s':"))
      )
    },

    test("should return this for root") {
      val messages = Map("test.key" -> "Test Message")
      val catalogue = new InMemoryMessageCatalogue(Language.EN, messages)

      assertTrue(catalogue.root eq catalogue)
    },

    test("should create nested catalogue") {
      val messages = Map(
        "prefix.key" -> "Prefixed Message",
        "key" -> "Base Message"
      )
      val catalogue = new InMemoryMessageCatalogue(Language.EN, messages)
      val nested = catalogue.nested("prefix")

      assertTrue(
        nested.get(MessageId("key")) == Some("Prefixed Message"),
        nested.get(MessageId("other")) == None
      )
    },

    test("should handle empty messages map") {
      val catalogue = new InMemoryMessageCatalogue(Language.EN, Map.empty)

      assertTrue(catalogue.get(MessageId("any.message")).isEmpty)
    },

    test("apply with fallback returns ID string when all missing") {
      val catalogue = new InMemoryMessageCatalogue(Language.EN, Map.empty)
      val missingId = MessageId("completely.missing")

      assertTrue(catalogue.apply(missingId) == missingId.toString)
    }
  )
end InMemoryMessageCatalogueSpec
