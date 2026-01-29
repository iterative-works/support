package works.iterative
package ui.components.tailwind.form

import com.raquo.laminar.api.L.*

import works.iterative.core.MessageId
import works.iterative.core.UserMessage

case class InvalidValue(message: UserMessage | HtmlElement)

object InvalidValue:
    def apply(message: MessageId): InvalidValue = InvalidValue(
        UserMessage(message)
    )
end InvalidValue
