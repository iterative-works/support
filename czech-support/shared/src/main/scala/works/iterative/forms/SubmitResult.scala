// PURPOSE: Result of submitting a form — success carries the submission id, confirmation email
// PURPOSE: and optional payment/redirect targets; failure carries user messages
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
