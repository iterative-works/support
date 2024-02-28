package works.iterative.autocomplete.ui.laminar

import com.raquo.laminar.api.L.*
import works.iterative.autocomplete.ui.AutocompleteComponents
import org.scalajs.dom.html.Element
import com.raquo.laminar.tags.HtmlTag
import com.raquo.laminar.nodes.ReactiveHtmlElement
import zio.*
import works.iterative.autocomplete.AutocompleteEntry
import works.iterative.ui.components.laminar.forms.FieldId
import works.iterative.ui.components.FileComponents

class LiveAutocompleteComponents(registry: AutocompleteRegistry, fileComponents: FileComponents)
    extends AutocompleteComponents:
    override def labelForAs[Ref <: Element](
        id: FieldId,
        value: String,
        as: HtmlTag[Ref]
    ): ReactiveHtmlElement[Ref] =
        val text: Var[Node] = Var(value)
        val link: Var[Option[String]] = Var(None)
        as(
            dataAttr("autocomplete_value")(value),
            registry.queryFor(id).load(value) --> Observer.combine[Option[AutocompleteEntry]](
                text.writer.contramap(_.map(_.label).getOrElse(value)),
                link.writer.contramap(_.flatMap(_.data.get(AutocompleteEntry.keys.href)))
            ),
            child <-- link.signal.map {
                case Some(hr) =>
                    a(
                        href(s"${fileComponents.baseHref}/${hr}"),
                        target("_blank"),
                        cls("cursor-pointer"),
                        child <-- text
                    )
                case None => span(child <-- text)
            }
        )
    end labelForAs
end LiveAutocompleteComponents

object LiveAutocompleteComponents:
    val layer: URLayer[AutocompleteRegistry & FileComponents, AutocompleteComponents] =
        ZLayer.derive[LiveAutocompleteComponents]
end LiveAutocompleteComponents
