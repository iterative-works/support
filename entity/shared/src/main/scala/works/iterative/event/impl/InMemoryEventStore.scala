package works.iterative.event
package impl

import zio.*

class InMemoryEventStore[Id, T <: Event[?]](data: Ref[Map[Id, List[T]]])
    extends EventStore[Id, T]:
    override def persist(id: Id, event: T): Op[Unit] =
        data.update { m =>
            val events = m.getOrElse(id, Nil)
            m.updated(id, event :: events)
        }.unit
    override def get(id: Id): Op[Seq[T]] =
        data.get.map(_.getOrElse(id, Nil))
end InMemoryEventStore

object InMemoryEventStore:
    def layer[Id: Tag, T <: Event[?]: Tag]: ULayer[EventStore[Id, T]] =
        ZLayer {
            for data <- Ref.make(Map.empty[Id, List[T]])
            yield InMemoryEventStore(data)
        }
end InMemoryEventStore
