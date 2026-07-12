// PURPOSE: The declared validation vocabulary a field carries in the form declaration
// PURPOSE: Closed common cases plus the open Rule escape hatch for client and async validations

package works.iterative.forms

import works.iterative.core.{MessageCatalogue, UserMessage}
import works.iterative.ui.model.forms.IdPath
import zio.prelude.*

enum Validation:
    case Required
    case Email
    case Pattern(regex: String)
    case MinLength(min: Int)
    case MaxLength(max: Int)
    case Rule(id: String, params: Map[String, String] = Map.empty)
end Validation

object Validation:
    private val emailShape = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$".r

    /** The pure format check of one declared validation against a field's raw value. Emptiness is
      * the Required concern and passes here; Rule validations resolve through the registry, and
      * unbound ids pass — they validate at other edges.
      */
    def check(
        validation: Validation,
        value: String,
        label: => String,
        registry: ValidationRuleRegistry = ValidationRuleRegistry.empty
    ): Option[UserMessage] =
        validation match
            case Required   => None
            case rule: Rule => registry.check(rule, value, label)
            case Email =>
                Option.unless(emailShape.matches(value))(UserMessage("error.field.email", label))
            case Pattern(regex) =>
                Option.unless(value.matches(regex))(UserMessage("error.field.pattern", label))
            case MinLength(min) =>
                Option.unless(value.length >= min)(
                    UserMessage("error.field.minlength", label, min)
                )
            case MaxLength(max) =>
                Option.unless(value.length <= max)(
                    UserMessage("error.field.maxlength", label, max)
                )

    /** The declared validations of one field as a composable rule, accumulating every failing check
      * at the field's path. Emptiness stays the composing interpreter's concern — a blank value
      * reaches this rule only when the field is optional and filled.
      */
    def rule[F[+_]: IdentityBoth: Covariant](
        id: IdPath,
        validations: List[Validation],
        registry: ValidationRuleRegistry = ValidationRuleRegistry.empty
    )(using
        MessageCatalogue
    ): ValidationRule[F, String, String] =
        ValidationRule.succeed: value =>
            validations.flatMap(check(_, value, id.toMessage("label"), registry)) match
                case Nil => ValidationState.Valid(value)
                case h :: t =>
                    ValidationState.Invalid(zio.NonEmptyChunk(id -> h, t.map(id -> _)*))
end Validation
