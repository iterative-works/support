package works.iterative.entity

import works.iterative.event.EventRecord

final case class ARCommand[Id, Command](id: Id, command: Command, record: EventRecord)

final case class AREvent[Id, Event](id: Id, event: Event, record: EventRecord)

final case class ARCommandResult[Id, Command, Event, State](
    command: ARCommand[Id, Command],
    originalState: State,
    newState: State,
    events: Seq[AREvent[Id, Event]]
)
