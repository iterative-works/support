package works.iterative
package ui

import core.{MessageCatalogue, MessageId}
import scala.scalajs.js
import works.iterative.core.UserMessage
import java.text.MessageFormat

// TODO: support hierarchical json structure
trait JsonMessageCatalogue extends MessageCatalogue:
  def messages: js.Dictionary[String]

  override def get(id: MessageId): Option[String] =
    assume(messages != null, "Message catalogue must not be null")
    messages.get(id.toString)

  override def get(msg: UserMessage): Option[String] =
    assume(messages != null, "Message catalogue must not be null")
    get(msg.id).map(_.format(msg.args: _*))
