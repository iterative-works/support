// PURPOSE: The declared validation vocabulary a field carries in the form declaration
// PURPOSE: Closed common cases plus the open Rule escape hatch for client and async validations

package works.iterative.forms

import works.iterative.core.UserMessage

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
      * the Required concern and Rule validations bind at the edges behind a registry, so both pass
      * here.
      */
    def check(validation: Validation, value: String, label: => String): Option[UserMessage] =
        validation match
            case Required | Rule(_, _) => None
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
end Validation
