package portaly.forms

import works.iterative.core.UserMessage
import works.iterative.ui.model.forms.AbsolutePath

trait FormValidationState:
    def isValid(id: AbsolutePath): Boolean
    def errors(id: AbsolutePath): List[UserMessage]

object FormValidationState:
    val valid: FormValidationState = new FormValidationState:
        override def isValid(id: AbsolutePath): Boolean = true
        override def errors(id: AbsolutePath): List[UserMessage] = Nil
end FormValidationState

class MapFormValidationState(
    state: Map[AbsolutePath, List[UserMessage]]
) extends FormValidationState:
    override def isValid(id: AbsolutePath): Boolean = state.get(id).forall(_.isEmpty)
    override def errors(id: AbsolutePath): List[UserMessage] = state.getOrElse(id, Nil)
end MapFormValidationState
