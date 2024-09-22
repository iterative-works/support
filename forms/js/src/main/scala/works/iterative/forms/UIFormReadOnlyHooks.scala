package portaly.forms

import com.raquo.laminar.api.L.*
import works.iterative.ui.model.forms.*

trait UIFormReadOnlyHooks:
    def adviceAroundSection(
        element: UIFormSection,
        data: FormState,
        render: (UIFormSection, FormState) => HtmlElement
    ): (UIFormSection, FormState) => HtmlElement = render

    def adviceAroundLabeledField(
        field: UILabeledField,
        data: FormState,
        render: (UILabeledField, FormState) => HtmlElement
    ): (UILabeledField, FormState) => HtmlElement = render

end UIFormReadOnlyHooks

object UIFormReadOnlyHooks:
    val empty: UIFormReadOnlyHooks = new UIFormReadOnlyHooks {}
end UIFormReadOnlyHooks
