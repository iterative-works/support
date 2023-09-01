package works.iterative
package core

// Type-wise naive solution for specifying user messages.
// A mechanism that will check the message for correct formatting and validate parameters is needed
// TODO: make UserMessage serializable
case class UserMessage(id: MessageId, args: Any*):
  override def toString(): String =
    s"${id}[${args.mkString(", ")}]"

object UserMessage:
  def withMessage(id: MessageId)(t: Throwable): UserMessage =
    UserMessage(id, t.getMessage())

  given Conversion[MessageId, UserMessage] with
    def apply(id: MessageId): UserMessage = UserMessage(id)
