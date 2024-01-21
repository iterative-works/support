package works.iterative.entity

import zio.*
import works.iterative.core.auth.CurrentUser

trait EntityCreateService[Id, Command, Event, State, Init]:
    type Op[A] = ZIO[CurrentUser, AggregateError[Id, Command, Event, State], A]

    def create(initData: Init): Op[Id]
end EntityCreateService

trait EntityUpdateService[Id, Command, Event, State]:
    type Op[A] = ZIO[CurrentUser, AggregateError[Id, Command, Event, State], A]

    def update(id: Id, command: Command): Op[Unit]
end EntityUpdateService

trait EntityService[Id, Command, Event, State, Init]
    extends EntityCreateService[Id, Command, Event, State, Init]
    with EntityUpdateService[Id, Command, Event, State]
end EntityService

trait EntityServiceAccessor[R, Id, Command, Event, State]:
    type Op[A] = ZIO[CurrentUser & R, AggregateError[Id, Command, Event, State], A]
    inline def delegate[A](inline f: R => ZIO[
        CurrentUser,
        AggregateError[Id, Command, Event, State],
        A
    ]): Op[A] =
        ZIO.serviceWithZIO[R](f)
end EntityServiceAccessor
