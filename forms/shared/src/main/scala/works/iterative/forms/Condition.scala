package portaly
package forms

import works.iterative.ui.model.forms.{AbsolutePath, IdPath}

sealed trait Condition
object Condition:
    case object Never extends Condition
    case object Always extends Condition
    case class AnyOf(conditions: Condition*) extends Condition
    case class AllOf(conditions: Condition*) extends Condition
    case class IsEqual(idp: String, value: String) extends Condition
    case class IsValid(idp: String) extends Condition
    case class NonEmpty(idp: String) extends Condition

    /** The one normative evaluation shared by every walker. Ids resolve against `base` (leading dot
      * \= absolute); `values` reads field state; `isValid` supplies field validity for IsValid —
      * callers evaluating without validation state (e.g. while computing it) pass `alwaysValid`.
      * NonEmpty treats blank strings as empty; empty combinators are total (AnyOf() = false,
      * AllOf() = true).
      */
    def eval(
        condition: Condition,
        base: AbsolutePath,
        values: AbsolutePath => Option[String],
        isValid: AbsolutePath => Boolean
    ): Boolean =
        def loop(condition: Condition): Boolean = condition match
            case Never              => false
            case Always             => true
            case AnyOf(conditions*) => conditions.exists(loop)
            case AllOf(conditions*) => conditions.forall(loop)
            case IsEqual(id, value) => values(IdPath.parse(id, base)).contains(value)
            case IsValid(id)        => isValid(IdPath.parse(id, base))
            case NonEmpty(id)       => values(IdPath.parse(id, base)).exists(!_.isBlank)
        loop(condition)
    end eval

    val alwaysValid: AbsolutePath => Boolean = _ => true
end Condition
