package works.iterative
package ui

import core.{MessageCatalogue, MessageId}
import scala.scalajs.js
import works.iterative.core.UserMessage
import java.text.MessageFormat

// TODO: support hierarchical json structure
trait JsonMessageCatalogue extends MessageCatalogue:
  def messages: js.Dictionary[String]

  override def apply(id: MessageId): Option[String] =
    messages.get(id.toString)

  override def apply(msg: UserMessage): Option[String] =
    apply(msg.id).map(_.format(msg.args: _*))
