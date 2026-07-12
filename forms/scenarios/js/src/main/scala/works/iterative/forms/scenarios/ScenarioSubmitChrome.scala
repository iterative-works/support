// PURPOSE: The scenario elements' submit chrome: a button that validates, posts and shows the dump
// PURPOSE: Decides validity only after the field-validation throttle settles, then posts JSON

package works.iterative.forms.scenarios

import com.raquo.laminar.api.L.*
import works.iterative.forms.ValidationState
import works.iterative.forms.impl.{FormR, LiveForm}
import zio.json.*

object ScenarioSubmitChrome:

    def wrap(
        form: LiveForm,
        submitButtonId: String,
        submitLabel: String,
        postUrl: String,
        receivedTitle: String
    ): HtmlElement =
        val submitted: Var[Option[String]] = Var(None)
        val clicks = new EventBus[Unit]
        // Field validation throttles at 500ms, so the state sampled right at the click can
        // trail the latest edits; decide once validation has settled
        val attempts = clicks.events.flatMapSwitch(_ =>
            EventStream.unit().delay(700).sample(form.data)
        )
        div(
            attempts.collect { case state if !state.isValid => true } --> form.showErrors,
            attempts.collect { case ValidationState.Valid(data) => (data: FormR).toJson }
                .flatMapSwitch(json =>
                    FetchStream.post(
                        postUrl,
                        _.body(json),
                        _.headers("Content-Type" -> "application/json")
                    )
                ).map(Some(_)) --> submitted.writer,
            child <-- submitted.signal.map {
                case Some(dumpJson) =>
                    div(
                        h1(receivedTitle),
                        p("Submitted data:"),
                        pre(code(dumpJson))
                    )
                case None =>
                    div(
                        form.element,
                        button(
                            idAttr(submitButtonId),
                            tpe("button"),
                            submitLabel,
                            onClick.mapToUnit --> clicks.writer
                        )
                    )
            }
        )
    end wrap
end ScenarioSubmitChrome
