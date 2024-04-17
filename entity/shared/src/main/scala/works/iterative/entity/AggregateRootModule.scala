package works.iterative.entity

import works.iterative.event.EventRecord
import works.iterative.core.UserMessage

trait AggregateRootModule[Id, Command, Event, State]:
    type ARCommand = works.iterative.entity.ARCommand[Id, Command]
    def ARCommand(id: Id, command: Command, record: EventRecord): ARCommand =
        works.iterative.entity.ARCommand(id, command, record)

    type AREvent = works.iterative.entity.AREvent[Id, Event]
    def AREvent(id: Id, event: Event, record: EventRecord): AREvent =
        works.iterative.entity.AREvent(id, event, record)

    type ARCommandResult = works.iterative.entity.ARCommandResult[Id, Command, Event, State]
    def ARCommandResult(
        command: ARCommand,
        originalState: State,
        newState: State,
        events: Seq[AREvent]
    ): ARCommandResult =
        works.iterative.entity.ARCommandResult(command, originalState, newState, events)

    type AggregateError = works.iterative.entity.AggregateError[Id, Command, Event, State]

    type CommandError = works.iterative.entity.CommandError[Id, Command, Event, State]
    type UnhandledCommand = works.iterative.entity.UnhandledCommand[Id, Command, Event, State]
    def UnhandledCommand(command: ARCommand, state: State): UnhandledCommand =
        works.iterative.entity.UnhandledCommand(command, state)
    type InvalidCommand = works.iterative.entity.InvalidCommand[Id, Command, Event, State]
    def InvalidCommand(command: ARCommand, state: State, message: UserMessage): InvalidCommand =
        works.iterative.entity.InvalidCommand(command, state, message)

    type EventError = works.iterative.entity.EventError[Id, Command, Event, State]
    type UnhandledEvent = works.iterative.entity.UnhandledEvent[Id, Command, Event, State]
    def UnhandledEvent(event: AREvent, state: State): UnhandledEvent =
        works.iterative.entity.UnhandledEvent(event, state)

    type FactoryError = works.iterative.entity.FactoryError[Id, Command, Event, State]
    type EntityNotFound = works.iterative.entity.EntityNotFound[Id, Command, Event, State]
    def EntityNotFound(entityId: String, id: Id): EntityNotFound =
        works.iterative.entity.EntityNotFound(entityId, id)
    type EntityAlreadyExists = works.iterative.entity.EntityAlreadyExists[Id, Command, Event, State]
    def EntityAlreadyExists(entityId: String, id: Id): EntityAlreadyExists =
        works.iterative.entity.EntityAlreadyExists(entityId, id)

    type ViewProcessor = works.iterative.entity.ViewProcessor[AREvent]
    type AggregateRoot = works.iterative.entity.AggregateRoot[Id, Command, Event, State]
    type AggregateRootFactory[T <: AggregateRoot] =
        works.iterative.entity.AggregateRootFactory[Id, Command, Event, State, T]

    type EntityCreateService[Init] =
        works.iterative.entity.EntityCreateService[Id, Command, Event, State, Init]
    type EntityUpdateService[Init] =
        works.iterative.entity.EntityUpdateService[Id, Command, Event, State]
    type EntityService[Init] = works.iterative.entity.EntityService[Id, Command, Event, State, Init]

    type EntityServiceAccessor[R] =
        works.iterative.entity.EntityServiceAccessor[R, Id, Command, Event, State]
end AggregateRootModule
