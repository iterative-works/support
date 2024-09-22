package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import works.iterative.autocomplete.AutocompleteEntry
import works.iterative.autocomplete.ui.laminar.AutocompleteViews
import works.iterative.autocomplete.ui.laminar.AutocompleteQuery

class AutocompleteFormField(
    initialValue: Option[String],
    query: AutocompleteQuery
)(using cs: AutocompleteViews)
    extends FormPart[Any, String, Boolean, String, Nothing, Option[AutocompleteEntry]]:
    type ThisInputs = FormPartInputs[Any, String, Boolean]
    type ThisOutputs = FormPartOutputs[String, Nothing, Option[AutocompleteEntry]]

    private val inputTouched: Var[Boolean] = Var(false)
    private val enabled: Var[Boolean] = Var(true)

    val touched: Signal[Boolean] = inputTouched.signal

    private def domOutput(fi: ThisInputs): (Var[Option[AutocompleteEntry]], HtmlElement) =
        val selectedValue: Var[Option[AutocompleteEntry]] = Var(None)

        val field = works.iterative.autocomplete.ui.laminar.AutocompleteFormField(
            fi.id.toHtmlId,
            fi.id.toHtmlName,
            query,
            initialValue,
            fi.errorInput.toSignal(false) && fi.showErrors,
            fi.rawInput,
            enabled.signal,
            onBlur.mapTo(true).compose(
                _.delay(700)
            ) --> inputTouched.writer.setDisplayName(
                s"touched onblur:${fi.id.toHtmlId}"
            ),
            query.strict
        )
        selectedValue -> field.element.amend(
            field.entry --> selectedValue.writer.debugWithName(
                s"writing ${fi.id.serialize}"
            ),
            fi.control.collect {
                case FormControl.Disable(p) if p == fi.id                => false
                case d: FormControl.DisableAll if d.path.contains(fi.id) => false
                case FormControl.Enable(p) if p == fi.id                 => true
                case e: FormControl.EnableAll if e.path.contains(fi.id)  => true
            } --> enabled.writer
        )
    end domOutput

    override def apply(fi: ThisInputs): ThisOutputs =
        val (selectedValue, elem) = domOutput(fi)
        FormPartOutputs(
            fi.id,
            selectedValue.signal.map(_.map(_.value).getOrElse("")).debugWithName(
                s"reading ${fi.id.serialize}"
            ),
            EventStream.empty,
            selectedValue.signal,
            elem
        )
    end apply
end AutocompleteFormField
