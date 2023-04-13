package works.iterative
package core

// Type-wise naive solution for specifying user messages.
// A mechanism that will check the message for correct formatting and validate parameters is needed
case class UserMessage(id: MessageId, args: Any*)

object UserMessage:
  given Conversion[MessageId, UserMessage] with
    def apply(id: MessageId): UserMessage = UserMessage(id)
