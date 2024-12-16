package works.iterative
package ui

import core.{MessageCatalogue, MessageId}
import works.iterative.core.UserMessage
import scala.util.Try
import zio.*
import zio.json.*
import works.iterative.core.Language

class InMemoryMessageCatalogue(override val language: Language, messages: Map[String, String])
    extends MessageCatalogue:

    override def get(id: MessageId): Option[String] =
        assume(messages != null, "Message catalogue must not be null")
        messages.get(id.toString)

    override def get(msg: UserMessage): Option[String] =
        assume(messages != null, "Message catalogue must not be null")
        get(msg.id).map(m =>
            Try(m.format(msg.args*)).fold(
                t => s"error formatting [${msg.id.toString()}]: '$m': ${t.getMessage}",
                identity
            )
        )
    end get

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
