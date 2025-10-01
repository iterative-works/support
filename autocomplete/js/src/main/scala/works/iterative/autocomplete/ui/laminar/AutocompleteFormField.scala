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
    inputFieldMod: HtmlMod = emptyMod,
    // Use only values from the option list
    strict: Boolean = false
)(using cs: AutocompleteViews):

    private val selectedValue: Var[Option[AutocompleteEntry]] = Var(None)

    val entry: Signal[Option[AutocompleteEntry]] = selectedValue.signal
    val value: Signal[Option[String]] = entry.map(_.map(_.value))

    val element: HtmlElement =
        val (autocompleteSource, autocompleteSink) =
            EventStream.withObserver[Seq[AutocompleteEntry]]
        val (values, valuesObserver) = EventStream.withObserver[String]

        def autocompleteSearchString(using Combobox.Ctx[AutocompleteEntry]) =
            // Get all the query changes
            Combobox.ctx.query.changes
                // Slow down the query to avoid too many requests
                .throttle(1000, false)
                // Combine with the focused signal, so that we react on focus change as well
                .combineWith(Combobox.ctx.isFocused.signal.changes)
                .filter(_._2)
                .map(_._1)
                // Combine with the selected field
                .withCurrentValueOf(selectedValue.signal)
                // to filter out the selected value from the query
                // it would return only one value, in such a case we want more options
                .map((q, v) => if v.map(_.label).contains(q) then "" else q)
                // And fire the query
                .flatMapSwitch(query.find)

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
                        autoComplete("off"),
                        inputFieldMod,
                        /* Strict needs to pick from the options, otherwise we load */
                        if !strict then
                            onBlur.compose(
                                _.sample(Combobox.ctx.query, selectedValue.signal).filter((q, v) =>
                                    !q.isBlank && !v.map(_.label).contains(q)
                                ).map(_._1)
                            ) --> valuesObserver
                        else emptyMod,
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
                autocompleteSearchString --> autocompleteSink,
                autocompleteSource --> Combobox.ctx.itemsWriter,
                // Sorting by label here breaks the specific order of the results
                // where weights are used
                // autocompleteSource.map(_.sortBy(_.label)) --> Combobox.ctx.itemsWriter,
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
