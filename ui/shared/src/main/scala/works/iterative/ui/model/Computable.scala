package works.iterative.ui.model

import works.iterative.core.UserMessage
import java.time.Instant
import zio.prelude.Covariant
import zio.prelude.Validation
import works.iterative.core.Validated
import zio.prelude.IdentityFlatten

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

    def isEmpty: Boolean = !this.isDefined

    def isComputing: Boolean = this match
        case Computable.Computing(_)      => true
        case Computable.Recomputing(_, _) => true
        case _                            => false
end Computable

object Computable:
    enum Update[+A]:
        case Loading extends Update[Nothing]
        case Done(value: A) extends Update[A]
        case Failed(msg: UserMessage) extends Update[Nothing]

    object Update:
        extension [A](u: Update[A])
            def apply(c: Computable[A]): Computable[A] = u match
                case Update.Loading     => c.started
                case Update.Done(v)     => c.update(v)
                case Update.Failed(msg) => c.fail(msg)
    end Update

    /** The initial state of a computable model
      */
    case object Uninitialized extends Computable[Nothing]:
        override def update[B](m: B): Computable[B] = Ready(m)
        override def started: Computable[Nothing] = Computing(Instant.now())

    /** The computation is in progress
      */
    case class Computing(start: Instant = Instant.now()) extends Computable[Nothing]:
        override def update[B](m: B): Computable[B] = Ready(m)
        override def started: Computable[Nothing] = this

    /** The computation is finished and the data is available
      */
    case class Ready[Model](model: Model) extends Computable[Model]:
        override def update[B >: Model](m: B): Computable[B] = Ready(m)
        override def started: Computable[Model] = Recomputing(Instant.now(), model)

    /** The computation is finished and the data is available, but it is being recomputed
      */
    case class Recomputing[Model](start: Instant, model: Model)
        extends Computable[Model]:
        override def update[B >: Model](m: B): Computable[B] = Ready(m)
        override def started: Computable[Model] = this
    end Recomputing

    /** The computation failed
      */
    case class Failed(error: UserMessage) extends Computable[Nothing]:
        override def update[B](m: B): Computable[B] = Ready(m)
        override def started: Computable[Nothing] = Computing(Instant.now())

    given IdentityFlatten[Computable] with
        def flatten[A](fa: Computable[Computable[A]]): Computable[A] = fa match
            case Uninitialized             => Uninitialized
            case Computing(start)          => Computing(start)
            case Ready(model)              => model
            case Failed(error)             => Failed(error)
            case Recomputing(start, model) => model
        def any: Computable[Any] = Uninitialized
    end given

    given Covariant[Computable] with
        def map[A, B](f: A => B): Computable[A] => Computable[B] =
            _ match
                case Uninitialized             => Uninitialized
                case Computing(start)          => Computing(start)
                case Ready(model)              => Ready(f(model))
                case Failed(error)             => Failed(error)
                case Recomputing(start, model) => Recomputing(start, f(model))
    end given

    extension [A](c: Computable[A])
        def toOption: Option[A] = c match
            case Ready(model)              => Some(model)
            case Recomputing(start, model) => Some(model)
            case _                         => None
end Computable
