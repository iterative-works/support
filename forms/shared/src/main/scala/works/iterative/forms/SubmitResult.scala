package portaly.forms

import works.iterative.core.Email
import works.iterative.core.UserMessage

sealed trait SubmitResult

object SubmitResult:
    case class Success(
        id: String,
        email: Option[Email],
        paymentUrl: Option[String],
        redirectUrl: Option[String]
    ) extends SubmitResult
    case class Failure(errors: Seq[UserMessage]) extends SubmitResult

    val UnknownFailure: SubmitResult = Failure(
        Seq(UserMessage("submit.failure.unknown"))
    )
end SubmitResult
