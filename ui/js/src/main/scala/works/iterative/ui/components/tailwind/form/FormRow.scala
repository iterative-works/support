package works.iterative.ui.components.tailwind.form

import com.raquo.laminar.api.L.*

case class FormRow(id: String, label: String, content: Modifier[Div])

object FormRow:

    extension (m: FormRow)
        def toHtml: HtmlElement =
            div(
                cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
                label(
                    forId := m.id,
                    cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                    m.label
                ),
                div(
                    cls := "mt-1 sm:mt-0 sm:col-span-2",
                    m.content
                )
            )
end FormRow
