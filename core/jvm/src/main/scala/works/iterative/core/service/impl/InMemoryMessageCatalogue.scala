// PURPOSE: MessageCatalogue implementation backed by pre-loaded in-memory Map
// PURPOSE: Provides pure synchronous access to messages without any IO during message lookup

package works.iterative
package core.service.impl

import works.iterative.core.{MessageCatalogue, MessageId}
import works.iterative.core.UserMessage
import scala.util.Try
import zio.*
import zio.json.*
import works.iterative.core.Language

/** MessageCatalogue implementation backed by pre-loaded in-memory Map.
  *
  * This implementation provides synchronous access to messages stored in an immutable Map. Messages
  * are typically loaded once (at startup or from resources) and stored for fast O(1) lookups.
  *
  * Key characteristics:
  *   - Pure synchronous access (no effects during message retrieval)
  *   - O(1) lookup performance using immutable Map
  *   - Thread-safe (immutable data structure)
  *   - Formatting errors return error message instead of throwing exceptions
  *
  * Usage:
  *   - With JSON resources: Use companion object's `fromJsonResources` method
  *   - With SQL database: Used by [[SqlMessageCatalogueService]] which manages pre-load and reload
  *     lifecycle
  *
  * @param language
  *   The language for this message catalogue
  * @param messages
  *   Pre-loaded map of message keys to message text
  *
  * @see
  *   [[SqlMessageCatalogueService]] for database-backed message catalogue service
  * @see
  *   [[InMemoryMessageCatalogueService]] for JSON resource-backed service
  */
class InMemoryMessageCatalogue(override val language: Language, messages: Map[String, String])
    extends MessageCatalogue:

    override def get(id: MessageId): Option[String] =
        messages.get(id.toString)

    override def get(msg: UserMessage): Option[String] =
        get(msg.id).map(template =>
            Try(template.format(msg.args*)).fold(
                exception => s"error formatting [${msg.id}]: '$template': ${exception.getMessage}",
                identity
            )
        )

    override val root: MessageCatalogue = this
end InMemoryMessageCatalogue

object InMemoryMessageCatalogue:
    def messagesFromJsonResources(lang: Option[Language]): UIO[MessageCatalogue] =
        val language = lang.getOrElse(Language.CS)
        val suffix = lang.filterNot(_ == Language.CS).map(l => s"_${l.value}").getOrElse("")
        ZIO.scoped {
            for
                resource <- ZIO.readURIInputStream(
                    getClass().getResource(s"/messages${suffix}.json").toURI()
                )
                content <- resource.readAll(2048).mapError {
                    case Some(e) => e
                    case None    => new IllegalStateException("No content for messages.json")
                }
                messages <- ZIO
                    .fromEither(
                        content.asString.fromJson[Map[String, String]]
                    )
                    .mapError(e =>
                        new IllegalStateException(
                            s"Failed to parse messages.json: $e"
                        )
                    )
            yield InMemoryMessageCatalogue(language, messages)
        }.orDie
    end messagesFromJsonResources

    val fromJsonResources: ULayer[MessageCatalogue] = fromJsonResources(None)

    def fromJsonResources(lang: Option[Language]): ULayer[MessageCatalogue] =
        ZLayer(messagesFromJsonResources(lang))
end InMemoryMessageCatalogue
