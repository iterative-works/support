package works.iterative.core
package service

import zio.*

/** Service for accessing message catalogues by language.
  *
  * This service provides a layer-based approach to managing message catalogues,
  * allowing different implementations for different deployment scenarios.
  *
  * Implementations:
  * - JSON-based (frontend/static): Messages bundled with application code, loaded from JSON files
  * - SQL-based (backend/dynamic): Messages pre-loaded from database at startup with hot reload support
  *
  * Choose JSON when:
  * - Messages rarely change and are managed by developers
  * - Simple deployment with no external dependencies
  * - Frontend applications (ScalaJS)
  *
  * Choose SQL when:
  * - Messages change frequently and need hot reload
  * - Non-technical users manage messages via admin UI
  * - Backend applications requiring runtime message updates
  *
  * @see [[works.iterative.sqldb.SqlMessageCatalogueService]] for SQL implementation with reload support
  */
trait MessageCatalogueService:
    def messages: UIO[MessageCatalogue]

    def forLanguage(language: Language): UIO[MessageCatalogue]
end MessageCatalogueService

object MessageCatalogueService:
    def messages: URIO[MessageCatalogueService, MessageCatalogue] =
        ZIO.serviceWithZIO(_.messages)

    def forLanguage(language: Language): URIO[MessageCatalogueService, MessageCatalogue] =
        ZIO.serviceWithZIO(_.forLanguage(language))

    val empty: MessageCatalogueService = new MessageCatalogueService:
        def messages: UIO[MessageCatalogue] = ZIO.succeed(MessageCatalogue.empty)

        def forLanguage(language: Language): UIO[MessageCatalogue] =
            ZIO.succeed(MessageCatalogue.empty)
    end empty
end MessageCatalogueService
