package works.iterative.core

import zio.prelude.*

type Validated[A] = ZValidation[UserMessage, UserMessage, A]

extension [A](v: Validated[A])
  def orThrow: A =
    v match
      case Validation.Success(_, a) => a
      case Validation.Failure(_, message) =>
        throw new IllegalArgumentException(message.head.id.toString())

object Validated:
  /** Validate and normalize nonempty string, returning "error.empty.$lkey" if
    * the string is empty, trimmed string otherwise
    */
  def nonEmptyString(lkey: String)(value: String): Validated[String] =
    Validation
      .fromPredicateWith(UserMessage(s"error.empty.$lkey"))(value)(s =>
        s != null && s.trim.nonEmpty
      )
      .map(_.trim)

  def nonNull[A](lkey: String)(value: A): Validated[A] =
    Validation
      .fromPredicateWith(UserMessage(s"error.null.$lkey"))(value)(_ != null)

  def positiveInt(lkey: String)(value: Int): Validated[Int] =
    Validation
      .fromPredicateWith(UserMessage(s"error.positive.$lkey"))(value)(_ > 0)
