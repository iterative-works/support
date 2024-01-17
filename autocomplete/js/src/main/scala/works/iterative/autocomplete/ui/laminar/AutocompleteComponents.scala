package works.iterative.autocomplete.ui

import com.raquo.laminar.api.L.*
import com.raquo.laminar.tags.HtmlTag
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import works.iterative.ui.components.laminar.forms.FieldId

trait AutocompleteComponents:
    def labelFor(id: FieldId, value: String): Span = labelForAs(id, value, span)

    def labelForAs[Ref <: dom.html.Element](
        id: FieldId,
        value: String,
        as: HtmlTag[Ref]
    ): ReactiveHtmlElement[Ref]
end AutocompleteComponents
