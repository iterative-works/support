package works.iterative.ui.components.laminar.tailwind.ui

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*

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
            if actions.isEmpty then emptyMod
            else
              nodeSeq(
                actions.head,
                actions.tail.map(_.amend(cls("ml-3")))
              )
          )
        )
      )

    def sectionWithSubtitle(
        title: Node,
        subtitle: Node,
        actions: HtmlElement*
    ): HtmlElement =
      div(
        cls("border-b border-gray-200 bg-white px-4 py-5 sm:px-6"),
        div(
          cls(
            "-ml-4 -mt-4 flex flex-wrap items-center justify-between sm:flex-nowrap"
          ),
          div(
            cls("ml-4 mt-4"),
            h3(
              cls("text-base font-semibold leading-6 text-gray-900"),
              title
            ),
            p(
              cls("mt-1 text-sm text-gray-500"),
              subtitle
            )
          ),
          div(
            cls("ml-4 mt-4 flex-shrink-0"),
            if actions.isEmpty then emptyMod
            else
              nodeSeq(
                actions.head,
                actions.tail.map(_.amend(cls("ml-3")))
              )
          )
        )
      )
