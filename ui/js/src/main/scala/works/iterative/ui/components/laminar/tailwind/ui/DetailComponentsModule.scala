package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext
import works.iterative.ui.model.FileRef

trait DetailComponentsModule:
  self: IconsModule =>
  object details:
    def section(
        title: Modifier[HtmlElement],
        subtitle: Option[Modifier[HtmlElement]],
        actions: Modifier[HtmlElement]*
    )(fields: HtmlElement*): HtmlElement =
      div(
        div(
          cls := "px-4 sm:px-0",
          h3(
            cls := "text-base font-semibold leading-7 text-gray-900",
            title
          ),
          subtitle.map(st =>
            p(
              cls := "mt-1 max-w-2xl text-sm leading-6 text-gray-500",
              st
            )
          )
          // TODO: actions
        ),
        div(
          cls := "mt-6 border-t border-gray-100",
          dl(
            cls := "divide-y divide-gray-100",
            fields
          )
        )
      )

    def field(
        title: String,
        content: Modifier[HtmlElement]*
    ): HtmlElement =
      div(
        cls := "px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0",
        dt(
          cls := "text-sm font-medium leading-6 text-gray-900",
          title
        ),
        dd(
          cls := "mt-1 text-sm leading-6 text-gray-700 sm:col-span-2 sm:mt-0",
          content
        )
      )

    def files(fs: List[FileRef]): HtmlElement =
      ul(
        role := "list",
        cls := "divide-y divide-gray-100 rounded-md border border-gray-200",
        fs.map(file)
      )

    def file(f: FileRef): HtmlElement =
      li(
        cls := "flex items-center justify-between py-4 pl-4 pr-5 text-sm leading-6",
        div(
          cls := "flex w-0 flex-1 items-center",
          icons.`paper-clip-solid`(),
          div(
            cls := "ml-4 flex min-w-0 flex-1 gap-2",
            span(
              cls := "truncate font-medium",
              f.name
            ),
            f.size.map(size => span(cls := "flex-shrink-0 text-gray-400", size))
          )
        ),
        div(
          cls := "ml-4 flex-shrink-0",
          a(
            href := "#",
            cls := "font-medium text-indigo-600 hover:text-indigo-500",
            "Uložit"
          )
        )
      )
