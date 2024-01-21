package works.iterative.entity

import zio.*
import works.iterative.core.service.IdGenerator

trait AggregateRootFactory[
    Id,
    Command,
    Event,
    State,
    T <: AggregateRoot[Id, Command, Event, State]
]:
    type NotFound = EntityNotFound[Id, Command, Event, State]
    type AlreadyExists = EntityAlreadyExists[Id, Command, Event, State]

    protected def AlreadyExists(id: Id): AlreadyExists =
        EntityAlreadyExists(entityId, id)

    protected def NotFound(id: Id): NotFound =
        EntityNotFound(entityId, id)

    def entityId: String
    def make(id: Id): IO[AlreadyExists, T]
    def load(id: Id): IO[NotFound, T]

    def loadOrMake(id: Id): UIO[T] =
        load(id)
            .orElse(make(id))
            .orDieWith: _ =>
                new RuntimeException(
                    s"Loading or making entity ${entityId} with id ${id} failed, loading fails with NotFound, making fails with AlreadyExists"
                )

    def make()(using idGen: IdGenerator[Id]): UIO[T] =
        val tryToCreate =
            for
                id <- idGen.nextId
                entity <- make(id)
            yield entity

        tryToCreate
            .retryN(10)
            .orDieWith: _ =>
                new RuntimeException(
                    "Cannot create entity, generator failed 10 times to create new ID"
                )
    end make
end AggregateRootFactory
