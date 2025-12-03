// PURPOSE: Comparison tests verifying SQL and JSON MessageCatalogue implementations are interchangeable
// PURPOSE: Ensures both implementations have identical behavior for message lookup, formatting, fallback, and nesting

package works.iterative.core.service

import zio.test.*
import zio.test.TestAspect.*
import works.iterative.core.{Language, MessageCatalogue, MessageId, UserMessage}
import works.iterative.core.service.impl.{InMemoryMessageCatalogue, SqlMessageCatalogue}

object MessageCatalogueComparisonSpec extends ZIOSpecDefault:

  /** Test data: identical messages for both implementations */
  val testMessages = Map(
    "simple.message" -> "Simple Message",
    "template.greeting" -> "Hello %s!",
    "template.multi" -> "User %s has %d points",
    "fallback.primary" -> "Primary Message",
    "fallback.secondary" -> "Secondary Message",
    "nested.module.feature.title" -> "Feature Title",
    "nested.module.feature.description" -> "Feature Description",
    "title" -> "Generic Title",
    "description" -> "Generic Description",
    "error.format.bad" -> "Value: %d %s %s" // Deliberately bad for error testing
  )

  def spec = suite("MessageCatalogueComparisonSpec")(

    test("both implementations return identical results for same message IDs") {
      val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
      val sql = new SqlMessageCatalogue(Language.EN, testMessages)

      val messageIds = Seq(
        MessageId("simple.message"),
        MessageId("fallback.primary"),
        MessageId("missing.message"),
        MessageId("nested.module.feature.title")
      )

      val inMemoryResults = messageIds.map(id => inMemory.get(id))
      val sqlResults = messageIds.map(id => sql.get(id))

      assertTrue(
        inMemoryResults == sqlResults,
        inMemoryResults.zip(sqlResults).forall { case (im, sq) => im == sq }
      )
    },

    test("formatting behavior matches exactly") {
      val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
      val sql = new SqlMessageCatalogue(Language.EN, testMessages)

      val userMessages = Seq(
        UserMessage(MessageId("template.greeting"), "Alice"),
        UserMessage(MessageId("template.multi"), "Bob", 100),
        UserMessage(MessageId("missing.template"), "Charlie"),
        UserMessage(MessageId("template.greeting")) // Missing argument
      )

      val inMemoryResults = userMessages.map(msg => inMemory.get(msg))
      val sqlResults = userMessages.map(msg => sql.get(msg))

      assertTrue(
        inMemoryResults == sqlResults,
        inMemoryResults.zip(sqlResults).forall { case (im, sq) => im == sq }
      )
    },

    test("formatting error handling matches") {
      val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
      val sql = new SqlMessageCatalogue(Language.EN, testMessages)

      // Test with wrong number of arguments (should produce error message)
      val badMessage = UserMessage(MessageId("error.format.bad"), "text") // Only 1 arg, needs 3

      val inMemoryResult = inMemory.get(badMessage)
      val sqlResult = sql.get(badMessage)

      assertTrue(
        inMemoryResult.isDefined,
        sqlResult.isDefined,
        inMemoryResult == sqlResult, // Error format must match exactly
        inMemoryResult.get.contains("error formatting"),
        sqlResult.get.contains("error formatting")
      )
    },

    test("fallback chain behavior matches") {
      val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
      val sql = new SqlMessageCatalogue(Language.EN, testMessages)

      // Test fallback from missing to existing
      val test1InMemory = inMemory.apply(MessageId("missing.primary"), MessageId("fallback.primary"))
      val test1Sql = sql.apply(MessageId("missing.primary"), MessageId("fallback.primary"))

      val test2InMemory = inMemory.apply(MessageId("missing.first"), MessageId("missing.second"), MessageId("fallback.secondary"))
      val test2Sql = sql.apply(MessageId("missing.first"), MessageId("missing.second"), MessageId("fallback.secondary"))

      val test3InMemory = inMemory.apply(MessageId("missing.all"), MessageId("also.missing"))
      val test3Sql = sql.apply(MessageId("missing.all"), MessageId("also.missing"))

      assertTrue(
        test1InMemory == test1Sql,
        test2InMemory == test2Sql,
        test3InMemory == test3Sql,
        test1InMemory == "Primary Message",
        test2Sql == "Secondary Message",
        test3InMemory == test3Sql // Both should return the ID string when all missing
      )
    },

    test("nested catalogue behavior matches") {
      val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
      val sql = new SqlMessageCatalogue(Language.EN, testMessages)

      // Create nested catalogues with same prefixes
      val inMemoryNested = inMemory.nested("nested.module.feature")
      val sqlNested = sql.nested("nested.module.feature")

      val messageIds = Seq(
        MessageId("title"), // Should find "nested.module.feature.title"
        MessageId("description"), // Should find "nested.module.feature.description"
        MessageId("missing") // Should fallback to unprefixed "missing" (doesn't exist)
      )

      val inMemoryResults = messageIds.map(id => inMemoryNested.get(id))
      val sqlResults = messageIds.map(id => sqlNested.get(id))

      assertTrue(
        inMemoryResults == sqlResults,
        inMemoryResults.zip(sqlResults).forall { case (im, sq) => im == sq },
        // Verify prefix worked
        inMemoryResults(0).contains("Feature Title"),
        sqlResults(0).contains("Feature Title")
      )
    },

    test("nested catalogue with fallback to unprefixed") {
      val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
      val sql = new SqlMessageCatalogue(Language.EN, testMessages)

      // Create nested with prefix that doesn't have all messages
      val inMemoryNested = inMemory.nested("other.prefix")
      val sqlNested = sql.nested("other.prefix")

      // Should fall back to unprefixed "title"
      val inMemoryResult = inMemoryNested.get(MessageId("title"))
      val sqlResult = sqlNested.get(MessageId("title"))

      assertTrue(
        inMemoryResult == sqlResult,
        inMemoryResult.contains("Generic Title"),
        sqlResult.contains("Generic Title")
      )
    },

    test("both handle missing messages identically") {
      val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
      val sql = new SqlMessageCatalogue(Language.EN, testMessages)

      val missingIds = Seq(
        MessageId("does.not.exist"),
        MessageId("also.missing"),
        MessageId("completely.absent")
      )

      val inMemoryResults = missingIds.map(id => inMemory.get(id))
      val sqlResults = missingIds.map(id => sql.get(id))

      assertTrue(
        inMemoryResults == sqlResults,
        inMemoryResults.forall(_.isEmpty),
        sqlResults.forall(_.isEmpty)
      )
    },

    test("apply with fallback returns ID string when all missing") {
      val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
      val sql = new SqlMessageCatalogue(Language.EN, testMessages)

      val missingId = MessageId("completely.missing")
      val inMemoryResult = inMemory.apply(missingId)
      val sqlResult = sql.apply(missingId)

      assertTrue(
        inMemoryResult == sqlResult,
        inMemoryResult == missingId.toString,
        sqlResult == missingId.toString
      )
    },

    test("both have same root reference") {
      val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
      val sql = new SqlMessageCatalogue(Language.EN, testMessages)

      assertTrue(
        inMemory.root eq inMemory,
        sql.root eq sql,
        inMemory.root.language == inMemory.language,
        sql.root.language == sql.language
      )
    },

    test("both handle empty messages map") {
      val inMemory = new InMemoryMessageCatalogue(Language.EN, Map.empty)
      val sql = new SqlMessageCatalogue(Language.EN, Map.empty)

      val id = MessageId("any.message")
      val inMemoryResult = inMemory.get(id)
      val sqlResult = sql.get(id)

      assertTrue(
        inMemoryResult == sqlResult,
        inMemoryResult.isEmpty,
        sqlResult.isEmpty
      )
    },

    test("property-based: random message IDs return same results") {
      check(Gen.string.filter(_.nonEmpty)) { randomKey =>
        val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
        val sql = new SqlMessageCatalogue(Language.EN, testMessages)

        val id = MessageId(randomKey)
        val inMemoryResult = inMemory.get(id)
        val sqlResult = sql.get(id)

        assertTrue(inMemoryResult == sqlResult)
      }
    },

    test("property-based: random formatting arguments return same results") {
      check(Gen.string.filter(_.nonEmpty), Gen.int) { (randomKey, randomArg) =>
        val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
        val sql = new SqlMessageCatalogue(Language.EN, testMessages)

        val msg = UserMessage(MessageId(randomKey), randomArg)
        val inMemoryResult = inMemory.get(msg)
        val sqlResult = sql.get(msg)

        assertTrue(inMemoryResult == sqlResult)
      }
    },

    test("property-based: opt returns same results for random message IDs") {
      check(Gen.string.filter(_.nonEmpty)) { randomKey =>
        val inMemory = new InMemoryMessageCatalogue(Language.EN, testMessages)
        val sql = new SqlMessageCatalogue(Language.EN, testMessages)

        val id = MessageId(randomKey)
        val inMemoryResult = inMemory.opt(id)
        val sqlResult = sql.opt(id)

        assertTrue(inMemoryResult == sqlResult)
      }
    }

  ) @@ sequential

end MessageCatalogueComparisonSpec
