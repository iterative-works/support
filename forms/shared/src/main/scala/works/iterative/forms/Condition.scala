package portaly
package forms

sealed trait Condition
object Condition:
    case object Never extends Condition
    case object Always extends Condition
    case class AnyOf(conditions: Condition*) extends Condition
    case class AllOf(conditions: Condition*) extends Condition
    case class IsEqual(idp: String, value: String) extends Condition
    case class IsValid(idp: String) extends Condition
    case class NonEmpty(idp: String) extends Condition
end Condition
