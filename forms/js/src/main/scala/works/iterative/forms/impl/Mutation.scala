package portaly.forms
package impl

import works.iterative.core.UserMessage
import works.iterative.ui.model.forms.IdPath

sealed trait InputMutation

object InputMutation:
    case class SetDefault[A](id: IdPath, value: Option[A]) extends InputMutation
    case class SetErrors(id: IdPath, value: Seq[UserMessage])
        extends InputMutation
    case class Set[A](id: IdPath, value: A) extends InputMutation
    case class SetAll(id: IdPath, value: Iterable[Set[?]]) extends InputMutation
    case object Noop extends InputMutation
    case object Init extends InputMutation
end InputMutation

sealed trait StateMutation

object StateMutation:
    case object Noop extends StateMutation
    case class Set[A](id: IdPath, value: A) extends StateMutation
    case class SetValidation[A](id: IdPath, state: ValidationState[A])
        extends StateMutation
end StateMutation
