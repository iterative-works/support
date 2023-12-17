package works.iterative
package ui

import core.{MessageCatalogue, MessageId}
import works.iterative.core.UserMessage
import scala.util.Try
import zio.*
import zio.json.*

class InMemoryMessageCatalogue(messages: Map[String, String])
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

object InMemoryMessageCatalogue:
  val fromJsonResources: ULayer[MessageCatalogue] =
    ZLayer.scoped {
      for
        resource <- ZIO.readURIInputStream(
          getClass().getResource("/messages.json").toURI()
        )
        content <- resource.readAll(2048).mapError {
          case Some(e) => e
          case None => new IllegalStateException("No content for messages.json")
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
      yield InMemoryMessageCatalogue(messages)
    }.orDie
