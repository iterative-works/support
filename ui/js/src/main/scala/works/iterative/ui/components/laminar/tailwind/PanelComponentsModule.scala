package works.iterative.ui.components.laminar.tailwind.ui

import com.raquo.laminar.api.L.*

trait PanelComponentsModule:
  object panel:
    def basicCard(content: HtmlMod*) =
      div(
        cls("bg-white overflow-hidden shadow rounded-lg"),
        div(cls("px-4 py-5 sm:p-6"), content)
      )

    /** Card, edge-to-edge on mobile */
    def cardEdgeToEdgeOnMobile(content: HtmlMod*) =
      div(
        cls("bg-white overflow-hidden shadow sm:rounded-lg"),
        div(cls("px-4 py-5 sm:p-6"), content)
      )

    def cardWithHeader(header: HtmlMod*)(content: HtmlMod*) =
      div(
        cls(
          "divide-y divide-gray-200 overflow-hidden rounded-lg bg-white shadow"
        ),
        div(
          cls("px-4 py-5 sm:px-6"),
          header
        ),
        div(
          cls("px-4 py-5 sm:p-6"),
          content
        )
      )
