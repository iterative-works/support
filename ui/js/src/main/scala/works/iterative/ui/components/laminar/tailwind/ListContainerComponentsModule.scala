package works.iterative.ui.components.laminar.tailwind.ui

import com.raquo.laminar.api.L.*

trait ListContainerComponentsModule:
  object listContainer:
    def simpleWithDividers(items: HtmlMod*) =
      ul(
        role("list"),
        cls("divide-y divide-gray-200"),
        items.map(li(cls("py-4"), _))
      )

    def cardWithDividers(items: HtmlMod*) =
      div(
        cls("overflow-hidden rounded-md bg-white shadow"),
        ul(
          role := "list",
          cls("divide-y divide-gray-200"),
          items.map(
            li(
              cls("px-6 py-4"),
              _
            )
          )
        )
      )
