package works.iterative.autocomplete
package ui
package laminar

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import works.iterative.ui.laminar.headless.Combobox
import com.raquo.laminar.nodes.TextNode

class AutocompleteFormField(
    fieldId: String,
    fieldName: String,
    query: AutocompleteQuery,
    initialValue: Option[String] = None,
    inError: Signal[Boolean] = Val(false),
    rawInput: EventStream[String] = EventStream.empty,
    enabled: Signal[Boolean] = Val(true),
    inputFieldMod: HtmlMod = emptyMod
)(using cs: AutocompleteViews):

    private val selectedValue: Var[Option[AutocompleteEntry]] = Var(None)

    val entry: Signal[Option[AutocompleteEntry]] = selectedValue.signal
    val value: Signal[Option[String]] = entry.map(_.map(_.value))

    val element: HtmlElement =
        val (source, sink) = EventStream.withObserver[Seq[AutocompleteEntry]]
        val (values, valuesObserver) = EventStream.withObserver[String]
        val initialized: Var[Boolean] = Var(false)
        Combobox[AutocompleteEntry](None)(
            cs.comboContainer(
                cs.inputFieldContainer(
                    inError,
                    Combobox.input(_.label)(cs.inputField(
                        fieldId,
                        fieldName,
                        inError,
                        tpe("text"),
                        inputFieldMod,
                        onBlur.compose(_.sample(Combobox.ctx.query, selectedValue.signal).filter(
                            (q, v) => !q.isBlank && !v.map(_.label).contains(q)
                        ).map(_._1)) --> valuesObserver,
                        readOnly <-- enabled.not,
                        disabled <-- enabled.not
                    )),
                    Combobox.button(cs.comboButton())
                ),
                Combobox.options(cs.comboOptionsContainer()): v =>
                    cs.comboOption(
                        Combobox.ictx.isActive,
                        Combobox.ictx.isSelected,
                        v.label,
                        v.text.map(TextNode(_))
                    ),
                Combobox.ctx.query.changes.throttle(1000, false).flatMapSwitch(query.find) --> sink,
                source --> Combobox.ctx.itemsWriter,
                Combobox.ctx.value --> selectedValue.writer,
                values.mapToTrue --> initialized.writer,
                values.delay(0).flatMapSwitch(query.load) --> Combobox.ctx.valueWriter,
                // Init the form field with default or empty string to start validation
                // Unless already initialized
                EventStream.fromValue(initialValue.getOrElse("")).filterWith(
                    initialized.signal.not
                ) --> valuesObserver,
                rawInput --> valuesObserver
            )
        )
    end element
end AutocompleteFormField
