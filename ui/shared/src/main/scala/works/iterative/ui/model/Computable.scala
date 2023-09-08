package works.iterative.ui.model

import works.iterative.core.UserMessage
import java.time.Instant
import zio.prelude.Covariant
import zio.prelude.Validation
import works.iterative.core.Validated

/** A class representing the states of a model that needs computation
  */
sealed trait Computable[+Model]:
  /** Update the computation state with new data
    */
  def update[B >: Model](m: B): Computable[B]

  def update[B >: Model](m: Validated[B]): Computable[B] =
    m match
      case Validation.Success(_, model)  => update(model)
      case Validation.Failure(_, errors) => fail(errors.head)

  /** Fail the computation */
  def fail(error: UserMessage): Computable[Nothing] = Computable.Failed(error)

  /** Mark the computation as started
    */
  def started: Computable[Model]

  def isFailed: Boolean = this match
    case Computable.Failed(_) => true
    case _                    => false

  def isDefined: Boolean = this match
    case Computable.Ready(_)          => true
    case Computable.Recomputing(_, _) => true
    case _                            => false

  def isComputing: Boolean = this match
    case Computable.Computing(_)      => true
    case Computable.Recomputing(_, _) => true
    case _                            => false

object Computable:
  /** The initial state of a computable model
    */
  case object Uninitialized extends Computable[Nothing]:
    override def update[B](m: B): Computable[B] = Ready(m)
    override def started: Computable[Nothing] = Computing(Instant.now())

  /** The computation is in progress
    */
  case class Computing(start: Instant) extends Computable[Nothing]:
    override def update[B](m: B): Computable[B] = Ready(m)
    override def started: Computable[Nothing] = this

  /** The computation is finished and the data is available
    */
  case class Ready[Model](model: Model) extends Computable[Model]:
    override def update[B >: Model](m: B): Computable[B] = Ready(m)
    override def started: Computable[Model] = Recomputing(Instant.now(), model)

  /** The computation is finished and the data is available, but it is being
    * recomputed
    */
  case class Recomputing[Model](start: Instant, model: Model)
      extends Computable[Model]:
    override def update[B >: Model](m: B): Computable[B] = Ready(m)
    override def started: Computable[Model] = this

  /** The computation failed
    */
  case class Failed(error: UserMessage) extends Computable[Nothing]:
    override def update[B](m: B): Computable[B] = Ready(m)
    override def started: Computable[Nothing] = Computing(Instant.now())

  given Covariant[Computable] with
    def map[A, B](f: A => B): Computable[A] => Computable[B] =
      _ match
        case Uninitialized             => Uninitialized
        case Computing(start)          => Computing(start)
        case Ready(model)              => Ready(f(model))
        case Failed(error)             => Failed(error)
        case Recomputing(start, model) => Recomputing(start, f(model))

  extension [A](c: Computable[A])
    def toOption: Option[A] = c match
      case Ready(model)              => Some(model)
      case Recomputing(start, model) => Some(model)
      case _                         => None
