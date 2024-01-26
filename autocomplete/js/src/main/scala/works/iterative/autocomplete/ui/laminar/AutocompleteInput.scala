package works.iterative.autocomplete.ui.laminar

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import works.iterative.autocomplete.AutocompleteEntry
import works.iterative.ui.components.laminar.forms.*
import works.iterative.core.Validated

class AutocompleteInput[A](
    desc: FieldDescriptor,
    initialValue: Option[String] = None,
    query: AutocompleteQuery,
    validation: Option[AutocompleteEntry] => Validated[A]
)(using AutocompleteViews) extends FormComponent[A]:
    private val inError: Var[Boolean] = Var(false)
    private val touched: Var[Boolean] = Var(false)

    private val field =
        AutocompleteFormField(
            desc.id,
            desc.name,
            query,
            initialValue = initialValue,
            inError = inError.signal && touched.signal,
            rawInput = EventStream.fromSeq(initialValue.toSeq),
            inputFieldMod = onBlur.mapTo(true) --> touched.writer
        )

    override val validated: Signal[Validated[A]] = field.entry.map(validation)

    override val elements: Seq[HtmlElement] = Seq(field.element.amend(
        validated --> inError.writer.contramap[Validated[A]](_.fold(_ => true, _ => false))
    ))
end AutocompleteInput
