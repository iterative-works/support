package works.iterative
package core

// Type-wise naive solution to speicifying user messages.
// A mechanism that will check the message for correct formatting and validate parameters is needed
case class UserMessage(id: MessageId, args: Any*)
