package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import works.iterative.core.*
import zio.*
import works.iterative.ui.model.forms.IdPath

type Validation = FormCtx ?=> ValidationRule[EventStream, String, String]
type SectionValidation = FormCtx ?=> ValidationRule[EventStream, FormR, FormR]

extension [A](z: Validated[A])
    def toValidationState(id: IdPath): ValidationState[A] =
        z.fold(err => ValidationState.Invalid(err.map(id -> _)), ValidationState.Valid(_))

object Validation:
    inline def apply(f: Validation): Validation = f

trait ValidationResolver:
    def resolve(fieldType: String): IdPath => Validation
    def resolveSection(sectionType: String): IdPath => SectionValidation

object ValidationResolver:
    val empty: ValidationResolver = new ValidationResolver:
        override def resolve(fieldType: String): IdPath => Validation =
            _ => ValidationRule.valid
        override def resolveSection(sectionType: String): IdPath => SectionValidation =
            _ => ValidationRule.valid
end ValidationResolver
