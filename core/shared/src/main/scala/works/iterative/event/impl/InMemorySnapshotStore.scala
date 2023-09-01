package works.iterative.event
package impl

import zio.*

class InMemorySnapshotStore[Id, T](data: Ref[Map[Id, T]])
    extends SnapshotStore[Id, T]:
  override def update(id: Id, snapshot: T): UIO[Unit] =
    data.update(_.updated(id, snapshot)).unit

  override def get(id: Id): UIO[Option[T]] =
    data.get.map(_.get(id))

object InMemorySnapshotStore:
  def layer[Id: Tag, T: Tag](): ULayer[SnapshotStore[Id, T]] =
    ZLayer {
      for data <- Ref.make(Map.empty[Id, T])
      yield InMemorySnapshotStore(data)
    }
