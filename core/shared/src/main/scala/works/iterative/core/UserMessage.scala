package works.iterative
package core

type MessageArg = String | Int | Long | Double | Boolean | Char

// Type-wise naive solution for specifying user messages.
// A mechanism that will check the message for correct formatting and validate parameters is needed
case class UserMessage(id: MessageId, args: MessageArg*):
    override def toString(): String =
        s"${id}[${args.mkString(", ")}]"

    inline def asString(using messages: MessageCatalogue): String =
        messages(this)

    inline def asOptionalString(using messages: MessageCatalogue): Option[String] =
        messages.get(this)
end UserMessage

object UserMessage:
    def withMessage(id: MessageId)(t: Throwable): UserMessage =
        UserMessage(id, t.getMessage())

    given Conversion[MessageId, UserMessage] with
        def apply(id: MessageId): UserMessage = UserMessage(id)
end UserMessage
