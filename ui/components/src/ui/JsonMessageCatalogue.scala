package works.iterative
package ui

import core.{MessageCatalogue, MessageId}
import scala.scalajs.js

// TODO: support hierarchical json structure
trait JsonMessageCatalogue extends MessageCatalogue:
  def messages: js.Dictionary[String]

  override def apply(id: MessageId): Option[String] =
    messages.get(id.toString)
