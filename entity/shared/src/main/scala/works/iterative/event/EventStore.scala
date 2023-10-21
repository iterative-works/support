package works.iterative.event

import zio.*

/** Abstraction of event log
  *
  * The log can persist events and restore the state of an entity from the
  * events.
  */
trait EventStore[Id, T <: Event[_]]:
  type Op[A] = UIO[A]
  def persist(id: Id, event: T): Op[Unit]
  def get(id: Id): Op[Seq[T]]
