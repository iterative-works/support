package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

trait TableComponentsModule:

  object tables:

    def tableSection(
        title: Modifier[HtmlElement],
        subtitle: Option[Modifier[HtmlElement]] = None,
        actions: Modifier[HtmlElement]*
    )(
        table: Modifier[HtmlElement]*
    ): HtmlElement =
      div(
        cls("px-4 sm:px-6 lg:px-8"),
        div(
          cls("sm:flex sm:items-center"),
          div(
            cls("sm:flex-auto"),
            h1(cls("text-base font-semibold leading-6 text-gray-900"), title),
            subtitle.map(st => p(cls("mt-2 text-sm text-gray-700"), st))
          ),
          div(cls("mt-4 sm:ml-16 sm:mt-0 sm:flex-none"), actions)
        ),
        table
      )

    def tableContainer(table: ReactiveHtmlElement[html.Table]): HtmlElement =
      div(
        cls("mt-8 flow-root"),
        div(
          cls("-mx-4 -my-2 overflow-x-auto sm:-mx-6 lg:-mx-8"),
          div(
            cls("inline-block min-w-full py-2 align-middle sm:px-6 lg:px-8"),
            table
          )
        )
      )

    def simpleTable(header: ReactiveHtmlElement[html.TableRow]*)(
        body: ReactiveHtmlElement[html.TableRow]*
    ): ReactiveHtmlElement[html.Table] =
      table(
        cls("min-w-full divide-y divide-gray-300"),
        thead(header),
        tbody(cls("divide-y divide-gray-200"), body)
      )

    def headerRow(
        mods: HtmlMod*
    )(
        cells: ReactiveHtmlElement[html.TableCell]*
    ): ReactiveHtmlElement[html.TableRow] =
      tr(
        cells.zipWithIndex.map((c, i) =>
          if i == 0 then c.amend(cls("py-3.5 pl-4 pr-3 sm:pl-0"))
          else if i == cells.length - 1 then
            c.amend(cls("py-3.5 pr-4 pl-3 sm:pr-0"))
          else c.amend(cls("py-3.5 px-3"))
        )
      )

    def dataRow(
        mods: HtmlMod*
    )(
        cells: ReactiveHtmlElement[html.TableCell]*
    ): ReactiveHtmlElement[html.TableRow] =
      tr(
        mods,
        cells.zipWithIndex.map((c, i) =>
          if i == 0 then c.amend(cls("py-4 pl-4 pr-3 sm:pl-0"))
          else if i == cells.length - 1 then
            c.amend(cls("py-4 pr-4 pl-3 sm:pr-0"))
          else c.amend(cls("py-4 px-3"))
        )
      )

    def headerCell(content: HtmlMod): ReactiveHtmlElement[html.TableCell] =
      th(
        cls("text-left text-sm font-semibold text-gray-900"),
        content
      )

    def dataCell(content: HtmlMod): ReactiveHtmlElement[html.TableCell] =
      td(
        cls("text-sm text-gray-500"),
        content
      )
