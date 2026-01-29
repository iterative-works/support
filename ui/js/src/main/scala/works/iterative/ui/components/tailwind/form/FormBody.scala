package works.iterative.ui.components.tailwind.form

import com.raquo.laminar.api.L.*

object FormBody:
    @deprecated("use specific form's 'body' method (see LabelsOnLeft)")
    def apply(sections: HtmlElement*): HtmlElement =
        div(
            cls := "space-y-8 divide-y divide-gray-200 sm:space-y-5",
            sections
        )
end FormBody
