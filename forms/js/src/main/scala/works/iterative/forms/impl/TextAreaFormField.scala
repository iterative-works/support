package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*

class TextAreaFormField(
    inputType: String,
    initialValue: Option[String],
    extraMods: HtmlMod*
)(using cs: Components) extends FormPart.StringInput:
    type ThisInputs = FormPartInputs[Any, String, Boolean]
    type ThisOutputs = FormPartOutputs[String, Nothing, String]

    private val inputValue: Var[String] = Var(initialValue.getOrElse(""))
    private val inputTouched: Var[Boolean] = Var(false)
    private val initialized: Var[Boolean] = Var(false)

    val touched: Signal[Boolean] = inputTouched.signal

    private def domOutput(fi: ThisInputs): HtmlElement =
        val inError = fi.errorInput.toSignal(false).setDisplayName(
            s"field_error:${fi.id.toHtmlId}"
        ) && fi.showErrors.setDisplayName(
            s"show_errors:${fi.id.toHtmlId}"
        )

        val htmlId = fi.id.toHtmlId

        cs.inputFieldContainer(
            inError,
            cs.textAreaField(
                htmlId,
                fi.id.toHtmlName,
                inError,
                tpe(inputType),
                controlled(
                    value <-- inputValue.signal,
                    onInput.mapToValue --> inputValue.writer
                ),
                inputValue.signal.mapTo(true) --> initialized.writer,
                inputValue.signal.changes.filterNot(_.isBlank).mapTo(true) --> inputTouched.writer,
                fi.rawInput.setDisplayName(
                    s"raw input:${htmlId}"
                ) --> inputValue.writer,
                onBlur.mapTo(true) --> inputTouched.writer,
                // Init the form field with default or empty string to start validation
                // Unless already initialized
                EventStream.fromValue(initialValue.getOrElse("")).filterWith(
                    initialized.signal.not
                ) --> inputValue.writer,
                extraMods
            )
        )
    end domOutput

    override def apply(fi: ThisInputs): ThisOutputs =
        FormPartOutputs.succeed(
            fi.id,
            inputValue.signal.setDisplayName(s"value:${fi.id.toHtmlId}"),
            domOutput(fi)
        )
end TextAreaFormField
