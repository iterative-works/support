package works.iterative.autocomplete.ui.laminar

import com.raquo.laminar.api.L.*
import works.iterative.autocomplete.ui.AutocompleteComponents
import org.scalajs.dom.html.Element
import com.raquo.laminar.tags.HtmlTag
import com.raquo.laminar.nodes.ReactiveHtmlElement
import zio.*
import works.iterative.autocomplete.AutocompleteEntry
import works.iterative.ui.components.laminar.forms.FieldId

class LiveAutocompleteComponents(registry: AutocompleteRegistry) extends AutocompleteComponents:
    override def labelForAs[Ref <: Element](
        id: FieldId,
        value: String,
        as: HtmlTag[Ref]
    ): ReactiveHtmlElement[Ref] =
        val text: Var[Node] = Var(value)
        as(
            child <-- text,
            dataAttr("autocomplete_value")(value),
            registry.queryFor(id).load(value) --> text.writer.contramap[Option[
                AutocompleteEntry
            ]](
                _.map(_.label).getOrElse(value)
            )
        )
    end labelForAs
end LiveAutocompleteComponents

object LiveAutocompleteComponents:
    val layer: URLayer[AutocompleteRegistry, AutocompleteComponents] =
        ZLayer.derive[LiveAutocompleteComponents]
end LiveAutocompleteComponents
