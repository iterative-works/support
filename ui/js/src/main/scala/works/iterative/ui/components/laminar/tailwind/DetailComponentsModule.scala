package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L.*
import works.iterative.core.FileRef
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.laminar.*
import works.iterative.core.UserMessage

trait DetailComponentsModule:
  self: IconsModule =>
  object details:
    def sectionHeader(
        title: Modifier[HtmlElement],
        subtitle: Option[Modifier[HtmlElement]],
        actions: Modifier[HtmlElement]*
    ): HtmlElement =
      div(
        cls(
          "flex flex-wrap items-center justify-between sm:flex-nowrap"
        ),
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
        ),
        div(cls("flex-shrink-0"), actions)
      )

    def section(
        title: Modifier[HtmlElement],
        subtitle: Option[Modifier[HtmlElement]],
        actions: Modifier[HtmlElement]*
    )(content: HtmlMod): HtmlElement =
      div(sectionHeader(title, subtitle, actions), content)

    def fields(items: Node*): HtmlElement =
      div(
        dl(
          cls := "divide-y divide-gray-100",
          items
        )
      )

    def field(
        title: Node,
        content: Modifier[HtmlElement]*
    ): HtmlElement =
      div(
        cls := "px-2 py-3 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0",
        dt(
          cls := "text-sm font-medium leading-6 text-gray-900",
          title
        ),
        dd(
          cls := "mt-1 text-sm leading-6 text-gray-700 sm:col-span-2 sm:mt-0",
          content
        )
      )

    def files(
        fs: Seq[FileRef],
        fileMods: Option[(FileRef, Int) => HtmlMod] = None
    )(
        mods: HtmlMod*
    )(using ComponentContext[?]): HtmlElement =
      ul(
        role := "list",
        cls := "divide-y divide-gray-100 rounded-md border border-gray-200",
        fs.zipWithIndex
          .map((f, i) =>
            fileMods match
              case Some(fm) => file(f)(fm(f, i))
              case _        => file(f)()
          ),
        mods
      )

    def file(f: FileRef)(mods: HtmlMod*)(using
        ComponentContext[?]
    ): HtmlElement =
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
            f.sizeString.map(size =>
              span(cls := "flex-shrink-0 text-gray-400", size)
            )
          )
        ),
        div(
          cls := "ml-4 flex-shrink-0",
          a(
            href := f.url,
            target := "_blank",
            cls := "font-medium text-indigo-600 hover:text-indigo-500",
            UserMessage("file.download").asString
          )
        ),
        mods
      )
