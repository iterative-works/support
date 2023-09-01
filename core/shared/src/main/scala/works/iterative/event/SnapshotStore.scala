package works.iterative.event

import zio.*

/** Snapshot store
  *
  * Stores a snapshot of an entity.
  */
trait SnapshotStore[Id, T]:
  type Op[A] = UIO[A]
  def update(id: Id, snapshot: T): Op[Unit]
  def get(id: Id): Op[Option[T]]
