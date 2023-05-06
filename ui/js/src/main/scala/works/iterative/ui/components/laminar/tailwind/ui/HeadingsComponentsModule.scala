package works.iterative.ui.components.laminar
package tailwind

import com.raquo.laminar.api.L.{*, given}

trait HeadingsComponentsModule:
  object headings:
    def section(title: Node, actions: HtmlElement*): HtmlElement =
      div(
        cls("border-b border-gray-200 pb-5"),
        div(
          cls("sm:flex sm:items-center sm:justify-between"),
          h3(cls("text-base font-semibold leading-6 text-gray-900"), title),
          div(
            cls("mt-3 flex sm:ml-4 sm:mt-0"),
            actions match
              case Nil => emptyMod
              case first :: rest =>
                first :: rest.map(_.amend(cls("ml-3")))
          )
        )
      )
