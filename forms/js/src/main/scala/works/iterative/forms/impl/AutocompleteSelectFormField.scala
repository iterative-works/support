package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import works.iterative.core.MessageCatalogue
import works.iterative.autocomplete.AutocompleteEntry
import com.raquo.laminar.nodes.TextNode

class AutocompleteSelectFormField(
    initialValue: Option[String],
    options: Signal[List[AutocompleteEntry]],
    initialDisabled: Boolean,
    extraMods: HtmlMod*
)(using cs: Components, messages: MessageCatalogue)
    extends FormPart[Any, String, Boolean, String, Nothing, Option[AutocompleteEntry]]:
    type ThisInputs = FormPartInputs[Any, String, Boolean]
    type ThisOutputs = FormPartOutputs[String, Nothing, Option[AutocompleteEntry]]

    private val inputTouched: Var[Boolean] = Var(false)
    private val initialized: Var[Boolean] = Var(false)
    private val enabled: Var[Boolean] = Var(!initialDisabled)

    val touched: Signal[Boolean] = inputTouched.signal

    private def domOutput(fi: ThisInputs): (Var[Option[AutocompleteEntry]], HtmlElement) =
        val inputValue: Var[String] = Var(initialValue.getOrElse(""))
        val selectedValue: Var[Option[AutocompleteEntry]] = Var(None)
        /*
        val inError = fi.errorInput.toSignal(false).setDisplayName(
            s"field_error:${fi.id.toHtmlId}"
        ) && fi.showErrors.setDisplayName(
            s"show_errors:${fi.id.toHtmlId}"
        )
         */

        val htmlId = fi.id.toHtmlId

        selectedValue -> cs.select(
            htmlId,
            fi.id.toHtmlName,
            fi.id.toMessageNode("label"),
            fi.id.toMessageNodeOpt("description"),
            enabled.signal,
            inputValue.signal,
            options.map(_.map: entry =>
                Components.RadioOption(
                    entry.value,
                    entry.value,
                    entry.label,
                    entry.text.map(TextNode(_))
                )),
            // On options change, set the value to the first option
            options.changes.withCurrentValueOf(inputValue.signal)
                .map((opts, v) => opts.find(_.value == v).orElse(opts.headOption).map(_.value) -> v)
                .collectOpt {
                    case (Some(v), cv) if v != cv => Some(v)
                    case _                        => None
                } --> inputValue.writer,
            disabled <-- enabled.signal.not,
            fi.control.collect {
                case FormControl.Disable(p) if p == fi.id                => false
                case d: FormControl.DisableAll if d.path.contains(fi.id) => false
                case FormControl.Enable(p) if p == fi.id                 => true
                case e: FormControl.EnableAll if e.path.contains(fi.id)  => true
            } --> enabled.writer,
            inputValue.signal.mapTo(true) --> initialized.writer,
            inputValue.signal.changes.filterNot(_.isBlank).mapTo(true) --> inputTouched.writer,
            inputValue.signal.combineWith(options).map {
                case (v, opts) => opts.find(_.value == v)
            }.withCurrentValueOf(selectedValue.signal).changes.filterNot((a, b) => a == b).map(
                _._1
            ) --> selectedValue.writer,
            fi.rawInput.setDisplayName(s"raw input:${htmlId}") --> inputValue.writer,
            onBlur.mapTo(true) --> inputTouched.writer,
            onChange.mapToValue --> inputValue.writer,
            // Init the form field with default or empty string to start validation
            // Unless already initialized
            EventStream.fromValue(initialValue.getOrElse("")).filterWith(
                initialized.signal.not
            ) --> inputValue.writer,
            extraMods
        )
    end domOutput

    override def apply(fi: ThisInputs): ThisOutputs =
        val (selectedValue, elem) = domOutput(fi)
        FormPartOutputs(
            fi.id,
            selectedValue.signal.map(_.map(_.value).getOrElse("")).setDisplayName(
                s"value:${fi.id.toHtmlId}"
            ),
            EventStream.empty,
            selectedValue.signal,
            elem
        )
    end apply
end AutocompleteSelectFormField
