// PURPOSE: Test suite for SqlMessageCatalogueService validating pre-load and reload functionality
// PURPOSE: Verifies cache management and message catalogue lifecycle

package works.iterative.core
package service.impl

import zio.*
import zio.test.*
import works.iterative.core.{Language, MessageId}
import works.iterative.core.model.MessageCatalogueData
import works.iterative.core.repository.MessageCatalogueRepository
import java.time.Instant

object SqlMessageCatalogueServiceSpec extends ZIOSpecDefault:

  def testRepository: UIO[MessageCatalogueRepository] =
    for
      state <- Ref.Synchronized.make(Map.empty[Language, Seq[MessageCatalogueData]])
    yield new MessageCatalogueRepository:
      def getAllForLanguage(language: Language): Task[Seq[MessageCatalogueData]] =
        state.get.map(_.getOrElse(language, Seq.empty))

      def bulkInsert(entities: Seq[MessageCatalogueData]): Task[Unit] =
        state.update { current =>
          val byLanguage = entities.groupBy(_.language)
          byLanguage.foldLeft(current) { case (acc, (lang, msgs)) =>
            acc.updated(lang, acc.getOrElse(lang, Seq.empty) ++ msgs)
          }
        }

  def spec = suite("SqlMessageCatalogueService")(
    test("should create service with empty cache from empty repository") {
      for
        repo <- testRepository
        service <- SqlMessageCatalogueService.make(repo, Seq(Language.EN), Language.EN)
        catalogue <- service.messages
        msg <- ZIO.succeed(catalogue.get(MessageId("test")))
      yield assertTrue(msg == None)
    },

    test("should return default language catalogue from messages") {
      for
        repo <- testRepository
        _ <- repo.bulkInsert(Seq(
          MessageCatalogueData(
            MessageId("test.key"),
            Language.EN,
            "Test Message",
            None,
            Instant.now(),
            Instant.now(),
            None,
            None
          )
        ))
        service <- SqlMessageCatalogueService.make(repo, Seq(Language.EN), Language.EN)
        catalogue <- service.messages
        msg <- ZIO.succeed(catalogue.get(MessageId("test.key")))
      yield assertTrue(
        msg == Some("Test Message"),
        catalogue.language == Language.EN
      )
    },

    test("should return specific language catalogue from forLanguage") {
      for
        repo <- testRepository
        _ <- repo.bulkInsert(Seq(
          MessageCatalogueData(
            MessageId("greet"),
            Language.CS,
            "Ahoj",
            None,
            Instant.now(),
            Instant.now(),
            None,
            None
          )
        ))
        service <- SqlMessageCatalogueService.make(repo, Seq(Language.CS, Language.EN), Language.EN)
        catalogue <- service.forLanguage(Language.CS)
        msg <- ZIO.succeed(catalogue.get(MessageId("greet")))
      yield assertTrue(
        msg == Some("Ahoj"),
        catalogue.language == Language.CS
      )
    },

    test("should reload single language when reload called with Some(language)") {
      for
        repo <- testRepository
        service <- SqlMessageCatalogueService.make(repo, Seq(Language.EN), Language.EN)
        catalogueBefore <- service.forLanguage(Language.EN)
        msgBefore <- ZIO.succeed(catalogueBefore.get(MessageId("new.key")))
        _ <- repo.bulkInsert(Seq(
          MessageCatalogueData(
            MessageId("new.key"),
            Language.EN,
            "New Message",
            None,
            Instant.now(),
            Instant.now(),
            None,
            None
          )
        ))
        _ <- service.reload(Some(Language.EN))
        catalogueAfter <- service.forLanguage(Language.EN)
        msgAfter <- ZIO.succeed(catalogueAfter.get(MessageId("new.key")))
      yield assertTrue(
        msgBefore == None,
        msgAfter == Some("New Message")
      )
    },

    test("should reload all languages when reload called with None") {
      for
        repo <- testRepository
        service <- SqlMessageCatalogueService.make(repo, Seq(Language.CS, Language.EN), Language.EN)
        _ <- repo.bulkInsert(Seq(
          MessageCatalogueData(
            MessageId("key.en"),
            Language.EN,
            "English Message",
            None,
            Instant.now(),
            Instant.now(),
            None,
            None
          ),
          MessageCatalogueData(
            MessageId("key.cs"),
            Language.CS,
            "Czech Message",
            None,
            Instant.now(),
            Instant.now(),
            None,
            None
          )
        ))
        _ <- service.reload(None)
        catalogueEN <- service.forLanguage(Language.EN)
        catalogueCS <- service.forLanguage(Language.CS)
        msgEN <- ZIO.succeed(catalogueEN.get(MessageId("key.en")))
        msgCS <- ZIO.succeed(catalogueCS.get(MessageId("key.cs")))
      yield assertTrue(
        msgEN == Some("English Message"),
        msgCS == Some("Czech Message")
      )
    }
  )
end SqlMessageCatalogueServiceSpec
