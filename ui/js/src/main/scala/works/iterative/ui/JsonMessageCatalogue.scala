package works.iterative
package ui

import core.{MessageCatalogue, MessageId}
import scala.scalajs.js
import works.iterative.core.UserMessage
import scala.util.Try
import works.iterative.core.Language

// TODO: support hierarchical json structure
// scalafix:off DisableSyntax.null
// JS interop: defensive check for js.Dictionary which can be null from JS side
trait JsonMessageCatalogue extends MessageCatalogue:
    def messages: js.Dictionary[String]

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
end JsonMessageCatalogue
// scalafix:on DisableSyntax.null

object JsonMessageCatalogue:
    def apply(lang: Language, msgs: js.Dictionary[String]): JsonMessageCatalogue =
        new JsonMessageCatalogue:
            override def language: Language = lang
            override def messages: js.Dictionary[String] = msgs
            override val root: MessageCatalogue = this
end JsonMessageCatalogue
