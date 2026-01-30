package works.iterative.core

import zio.prelude.*

type Validated[A] = ZValidation[UserMessage, UserMessage, A]

object ValidatedSyntax:
    extension [A](v: Validated[A])
        // scalafix:off DisableSyntax.throw
        // Intentional: orThrow is an escape hatch for validated values
        def orThrow: A =
            v match
                case Validation.Success(_, a) => a
                case Validation.Failure(_, message) =>
                    throw new IllegalArgumentException(message.head.id.toString())
        // scalafix:on DisableSyntax.throw
end ValidatedSyntax

object Validated:
    /** Validate and normalize nonempty string, returning "error.empty.$lkey" if the string is
      * empty, trimmed string otherwise
      */
    // scalafix:off DisableSyntax.null
    // Intentional: null check is the purpose of this validation function
    def nonEmptyString(lkey: String)(value: String): Validated[String] =
        Validation
            .fromPredicateWith(UserMessage(s"error.empty.$lkey"))(value)(s =>
                s != null && s.trim.nonEmpty
            )
            .map(_.trim)

    def nonNull[A](lkey: String)(value: A): Validated[A] =
        Validation
            .fromPredicateWith(UserMessage(s"error.null.$lkey"))(value)(_ != null)
    // scalafix:on DisableSyntax.null

    def positiveInt(lkey: String)(value: Int): Validated[Int] =
        Validation
            .fromPredicateWith(UserMessage(s"error.positive.$lkey"))(value)(_ > 0)
end Validated
