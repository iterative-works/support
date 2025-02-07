package works.iterative.core
package service
package impl

import zio.*

// TODO: add caching
class InMemoryMessageCatalogueService extends MessageCatalogueService:
    override def messages: UIO[MessageCatalogue] =
        InMemoryMessageCatalogue.messagesFromJsonResources(None)

    override def forLanguage(language: Language): UIO[MessageCatalogue] =
        InMemoryMessageCatalogue.messagesFromJsonResources(Some(language))
end InMemoryMessageCatalogueService

object InMemoryMessageCatalogueService:
    val layer: ZLayer[Any, Nothing, InMemoryMessageCatalogueService] =
        ZLayer.succeed(new InMemoryMessageCatalogueService())
