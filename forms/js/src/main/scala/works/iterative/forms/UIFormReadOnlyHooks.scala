package portaly.forms

import com.raquo.laminar.api.L.*
import works.iterative.ui.model.forms.*

trait UIFormReadOnlyHooks:
    self =>
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

    def composeWith(other: UIFormReadOnlyHooks): UIFormReadOnlyHooks =
        new UIFormReadOnlyHooks:
            override def adviceAroundSection(
                element: UIFormSection,
                data: FormState,
                render: (UIFormSection, FormState) => HtmlElement
            ): (UIFormSection, FormState) => HtmlElement =
                self.adviceAroundSection(
                    element,
                    data,
                    other.adviceAroundSection(element, data, render)
                )

            override def adviceAroundLabeledField(
                field: UILabeledField,
                data: FormState,
                render: (UILabeledField, FormState) => HtmlElement
            ): (UILabeledField, FormState) => HtmlElement =
                self.adviceAroundLabeledField(
                    field,
                    data,
                    other.adviceAroundLabeledField(field, data, render)
                )
        end new
    end composeWith
end UIFormReadOnlyHooks

object UIFormReadOnlyHooks:
    val empty: UIFormReadOnlyHooks = new UIFormReadOnlyHooks {}
end UIFormReadOnlyHooks
