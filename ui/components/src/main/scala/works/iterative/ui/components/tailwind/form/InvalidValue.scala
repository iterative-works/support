package works.iterative
package ui.components.tailwind.form

import works.iterative.core.MessageId
import works.iterative.core.UserMessage

case class InvalidValue(message: UserMessage)

object InvalidValue {
  def apply(message: MessageId): InvalidValue = InvalidValue(
    UserMessage(message)
  )
}
