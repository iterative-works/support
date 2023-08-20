package works.iterative
package core

// Type-wise naive solution for specifying user messages.
// A mechanism that will check the message for correct formatting and validate parameters is needed
// TODO: make UserMessage serializable
case class UserMessage(id: MessageId, args: Any*):
  override def toString(): String =
    s"${id}[${args.mkString(", ")}]"

object UserMessage:
  given Conversion[MessageId, UserMessage] with
    def apply(id: MessageId): UserMessage = UserMessage(id)
