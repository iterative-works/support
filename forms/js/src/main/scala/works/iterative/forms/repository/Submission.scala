package portaly.forms
package repository

import java.time.OffsetDateTime
import works.iterative.core.Email
import works.iterative.ui.model.forms.IdPath

final case class Submission(
    id: String,
    value: FormContent,
    createdAt: OffsetDateTime,
    paid: Option[OffsetDateTime] = None,
    paymentId: Option[String] = None,
    paymentUrl: Option[String] = None,
    strediska: List[String] = Nil,
    stav: Option[String] = None
):
    val zavazna =
        value.firstString(Submission.keys.zavazna).flatMap(_.toBooleanOption).getOrElse(false)

    def email: Option[Email] = value.firstString(Submission.keys.email).flatMap(Email(_).toOption)

    def vs: String = Submission.vs(id)
end Submission

object Submission:
    object keys:
        val email: FormKey = IdPath.Root / "rmv" / "zadatel" / "kontaktni_osoba" / "email"
        val zavazna: FormKey = FormKey.unsafe("zavazna")

    def vs(id: String): String =
        id.split("-") match
            case Array(_, _, c, y) => s"$y${c.drop(1)}"
            case _                 => "10"
end Submission
