package works.iterative.entity

import zio.*

trait AggregateRoot[Id, Command, Event, State]:
    def id: Id
    def state: UIO[State]
    def handle(command: ARCommand[Id, Command])
        : IO[AggregateError[Id, Command, Event, State], ARCommandResult[Id, Command, Event, State]]
end AggregateRoot
