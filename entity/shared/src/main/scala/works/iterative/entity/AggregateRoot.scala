package works.iterative.entity

import zio.*

/** Represents aggregate root in DDD
  *
  * @tparam Id
  *   Aggregate root id
  * @tparam Command
  *   Aggregate root command
  * @tparam Event
  *   Aggregate root event
  * @tparam State
  *   Aggregate root state
  */
trait AggregateRoot[Id, Error <: AggregateError, Command, Event, State]:
  /** Aggregate root id */
  def id: Id

  /** Current state */
  def state: UIO[State]

  def handle(command: Command): IO[Error, Unit]
