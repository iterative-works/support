package works.iterative.ui.components.tailwind.form

import com.raquo.laminar.api.L.*

case class FormPropertyRow[V](
    property: FormProperty[V],
    input: Property[V] => Modifier[Div]
)

object FormPropertyRow:
  extension [V](m: FormPropertyRow[V])
    def element: HtmlElement =
      div(
        cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
        label(
          forId := m.property.id,
          cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
          m.property.label
        ),
        div(
          cls := "mt-1 sm:mt-0 sm:col-span-2",
          m.input(m.property),
          m.property.description.map(d =>
            p(cls := "mt-2 text-sm text-gray-500", d)
          )
        )
      )
