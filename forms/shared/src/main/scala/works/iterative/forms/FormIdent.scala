package portaly.forms

import zio.prelude.*
import works.iterative.core.ValidatedStringFactory
import works.iterative.core.UserMessage
import works.iterative.core.Validated

opaque type FormIdent = String

object FormIdent extends ValidatedStringFactory[FormIdent](identity):
    def apply(value: String): Validated[FormIdent] =
        Validation.fromPredicateWith(UserMessage("error.invalid.format"))(value)(_.contains(":"))

    def apply(entityId: String, formId: String): FormIdent =
        s"$entityId:$formId"

    def apply(entityId: String, formId: String, formIndex: Int): FormIdent =
        s"$entityId:$formId:$formIndex"

    extension (fi: FormIdent)
        def entityId: String = fi.split(":").head
        def formId: String = fi.split(":").tail.head
        def formIndex: Option[Int] = fi.split(":").tail.tail.headOption.map(_.toInt)
    end extension
end FormIdent
