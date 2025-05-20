package works.iterative.ui.components.tailwind.form

import com.raquo.laminar.api.L.*

object Form:
    val Body = FormBody
    val Section = FormSection
    val Row = FormRow

    @deprecated(
        "use specific form instance's form method (see form.LabelsOnLeft)"
    )
    def apply(body: HtmlElement, buttons: HtmlElement): HtmlElement =
        form(
            cls := "space-y-8 divide-y divide-gray-200",
            body,
            buttons
        )
end Form
