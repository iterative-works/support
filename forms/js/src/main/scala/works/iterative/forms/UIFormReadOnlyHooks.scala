package portaly.forms

import com.raquo.laminar.api.L.*
import works.iterative.ui.model.forms.*

trait UIFormReadOnlyHooks:
    def renderSection(
        element: UIFormSection,
        data: FormState,
        render: (UIFormSection, FormState) => HtmlElement
    ): HtmlElement = render(element, data)

    def renderLabeledField(
        field: UILabeledField,
        data: FormState,
        render: (UILabeledField, FormState) => HtmlElement
    ): HtmlElement = render(field, data)

end UIFormReadOnlyHooks

object UIFormReadOnlyHooks:
    val empty: UIFormReadOnlyHooks = new UIFormReadOnlyHooks {}
end UIFormReadOnlyHooks
