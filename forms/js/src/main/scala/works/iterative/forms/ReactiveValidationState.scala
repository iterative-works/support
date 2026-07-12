// PURPOSE: Reactive validation helpers bound to the browser event stream
// PURPOSE: Hosts the deprecated required-gating helper over EventStream validations

package works.iterative.forms

import com.raquo.airstream.core.EventStream
import works.iterative.core.{MessageCatalogue, UserMessage}
import works.iterative.ui.model.forms.IdPath

object ReactiveValidationState:
    @deprecated
    def required[OutputValue](id: IdPath, inp: String, required: Boolean)(using
        MessageCatalogue
    )(
        otherValidations: => EventStream[ValidationState[OutputValue]]
    ): EventStream[ValidationState[OutputValue]] =
        if inp.isBlank && required then
            EventStream.fromValue(
                ValidationState.Invalid(
                    id,
                    UserMessage("error.field.required", id.toMessage("label"))
                )
            )
        else otherValidations
end ReactiveValidationState
