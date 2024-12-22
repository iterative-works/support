package works.iterative.entity

import works.iterative.core.UserMessage
import works.iterative.core.MessageId

sealed trait AggregateError[Id, Command, Event, State]:
    def userMessage: UserMessage

case class InvalidRequest[Id, Command, Event, State](message: UserMessage)
    extends AggregateError[Id, Command, Event, State]:
    val userMessage: UserMessage = message

sealed trait CommandError[Id, Command, Event, State]
    extends AggregateError[Id, Command, Event, State]

case class UnhandledCommand[Id, Command, Event, State](
    command: ARCommand[Id, Command],
    state: State
) extends CommandError[Id, Command, Event, State]:
    val userMessage: UserMessage =
        UserMessage("error.unhandled.command", command.toString(), state.toString())
end UnhandledCommand

case class InvalidCommand[Id, Command, Event, State](
    command: ARCommand[Id, Command],
    state: State,
    message: UserMessage
) extends CommandError[Id, Command, Event, State]:
    val userMessage: UserMessage = message
end InvalidCommand

sealed trait EventError[Id, Command, Event, State] extends AggregateError[Id, Command, Event, State]

case class UnhandledEvent[Id, Command, Event, State](event: AREvent[Id, Event], state: State)
    extends EventError[Id, Command, Event, State]:
    val userMessage: UserMessage =
        UserMessage("error.unhandled.event", event.toString(), state.toString())
end UnhandledEvent

sealed trait FactoryError[Id, Command, Event, State]
    extends AggregateError[Id, Command, Event, State]:
    def entityId: String
    def userMessage: UserMessage
end FactoryError

case class EntityAlreadyExists[Id, Command, Event, State](entityId: String, id: Id)
    extends FactoryError[Id, Command, Event, State]:
    def userMessage = UserMessage(s"${entityId}.error.entity.exists", id.toString())

case class EntityNotFound[Id, Command, Event, State](entityId: String, id: Id)
    extends FactoryError[Id, Command, Event, State]:
    def userMessage = UserMessage(s"${entityId}.error.entity.not.found", id.toString())
