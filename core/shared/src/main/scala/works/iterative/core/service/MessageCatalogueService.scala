package works.iterative.core
package service

import zio.*

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
