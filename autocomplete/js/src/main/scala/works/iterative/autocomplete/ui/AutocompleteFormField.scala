package works.iterative.autocomplete.ui

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import works.iterative.ui.laminar.headless.Combobox
import com.raquo.laminar.nodes.TextNode
import works.iterative.autocomplete.AutocompleteEntry

class AutocompleteFormField(
    fieldId: String,
    fieldName: String,
    // Find by label
    find: String => EventStream[Seq[AutocompleteEntry]],
    // Load by value, used when setting from outside
    load: String => EventStream[Option[AutocompleteEntry]],
    strict: Boolean = true,
    initialValue: Option[String] = None,
    inError: Signal[Boolean] = Val(false),
    rawInput: EventStream[String] = EventStream.empty,
    inputFieldMod: HtmlMod = emptyMod
)(using cs: AutocompleteViews):

    private val selectedValue: Var[Option[AutocompleteEntry]] = Var(None)

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
                        ).map(_._1)) --> valuesObserver
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
                Combobox.ctx.query.changes.throttle(1000, false).flatMap(find) --> sink,
                source --> Combobox.ctx.itemsWriter,
                Combobox.ctx.value --> selectedValue.writer,
                values.mapToTrue --> initialized.writer,
                values.flatMap(v => load(v).map(v -> _)).collectOpt {
                    // Do not reset value if it is not found, if not strict
                    case (v, None) if !strict =>
                        Some(Some(AutocompleteEntry(v, v, None, Map.empty)))
                    case (_, r) => Some(r)
                } --> Combobox.ctx.valueWriter,
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
