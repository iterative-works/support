// PURPOSE: MessageCatalogueService implementation with database-backed pre-loaded cache
// PURPOSE: Manages message catalogue lifecycle with pre-load at startup and reload capability

package works.iterative.core
package service.impl

import zio.*
import works.iterative.core.repository.MessageCatalogueRepository
import works.iterative.core.service.MessageCatalogueService

/** MessageCatalogueService that pre-loads messages from database at startup.
  * Messages are stored in an in-memory cache (Ref) for fast synchronous access.
  * Supports reloading messages from database for specific languages or all configured languages.
  *
  * @param repository The repository for loading messages from database
  * @param cacheRef Reference to the in-memory cache of messages by language
  * @param defaultLanguage The default language to use when messages() is called
  */
class SqlMessageCatalogueService(
    repository: MessageCatalogueRepository,
    cacheRef: Ref[Map[Language, Map[String, String]]],
    defaultLanguage: Language
) extends MessageCatalogueService:

  override def messages: UIO[MessageCatalogue] =
    forLanguage(defaultLanguage)

  override def forLanguage(language: Language): UIO[MessageCatalogue] =
    cacheRef.get.map { cache =>
      val messages = cache.getOrElse(language, Map.empty)
      new SqlMessageCatalogue(language, messages)
    }

  /** Reloads messages from the database for specified language(s).
    * This method is on the concrete class (not MessageCatalogueService trait)
    * because the JSON implementation cannot reload messages from files.
    *
    * @param language None to reload all configured languages, Some(lang) to reload specific language
    * @return Task that completes when reload is finished
    */
  def reload(language: Option[Language]): Task[Unit] =
    language match
      case Some(lang) =>
        for
          entities <- repository.getAllForLanguage(lang)
          messages = entities.map(e => e.messageKey.toString -> e.messageText).toMap
          _ <- ZIO.logInfo(s"Reloading $lang: ${messages.size} messages")
          _ <- cacheRef.update(cache => cache.updated(lang, messages))
        yield ()

      case None =>
        for
          currentCache <- cacheRef.get
          languages = currentCache.keys.toSeq
          _ <- ZIO.logInfo(s"Reloading all languages: ${languages.mkString(", ")}")
          languageData <- ZIO.foreachPar(languages) { lang =>
            repository.getAllForLanguage(lang).map(entities =>
              lang -> entities.map(e => e.messageKey.toString -> e.messageText).toMap
            )
          }
          newCache = languageData.toMap
          _ <- cacheRef.set(newCache)
          totalMessages = newCache.values.map(_.size).sum
          _ <- ZIO.logInfo(s"Reloaded ${languages.size} languages: $totalMessages total messages")
        yield ()
end SqlMessageCatalogueService

object SqlMessageCatalogueService:
  /** Creates a SqlMessageCatalogueService with pre-loaded messages.
    * This method loads all messages for specified languages at creation time.
    * Use .orDie in layer to ensure application fails fast if messages cannot be loaded.
    *
    * @param repository The repository for loading messages
    * @param languages Languages to pre-load at startup
    * @param defaultLanguage The default language for the service
    * @return Task containing the service with pre-loaded messages
    */
  def make(
      repository: MessageCatalogueRepository,
      languages: Seq[Language],
      defaultLanguage: Language
  ): Task[SqlMessageCatalogueService] =
    for
      _ <- ZIO.logInfo(s"Pre-loading message catalogues for ${languages.size} languages")
      languageData <- ZIO.foreachPar(languages) { lang =>
        for
          entities <- repository.getAllForLanguage(lang)
          messages = entities.map(e => e.messageKey.toString -> e.messageText).toMap
          _ <- ZIO.logInfo(s"Loaded $lang: ${messages.size} messages")
        yield lang -> messages
      }
      cache <- Ref.make(languageData.toMap)
      totalMessages = languageData.map(_._2.size).sum
      _ <- ZIO.logInfo(s"Message catalogue service initialized: $totalMessages total messages")
    yield new SqlMessageCatalogueService(repository, cache, defaultLanguage)

  /** Creates a ZIO layer for SqlMessageCatalogueService.
    * Uses .orDie for fail-fast behavior - the application will not start if messages cannot be loaded.
    * This ensures the application has all required messages available before serving requests.
    *
    * @param languages Languages to pre-load at startup
    * @param defaultLanguage The default language for the service
    * @return Layer that provides MessageCatalogueService
    */
  def layer(
      languages: Seq[Language],
      defaultLanguage: Language
  ): URLayer[MessageCatalogueRepository, MessageCatalogueService] =
    ZLayer.fromZIO(
      ZIO.serviceWithZIO[MessageCatalogueRepository](repo =>
        make(repo, languages, defaultLanguage).orDie
      )
    )
end SqlMessageCatalogueService
