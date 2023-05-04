package works.iterative.ui.components.tailwind
package list

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.raquo.laminar.nodes.ReactiveHtmlElement

trait AsListRow[A]:
  extension (a: A) def asListRow: ListRow

final case class ListRow(
    title: HtmlElement,
    topRight: Modifier[HtmlElement],
    bottomLeft: Modifier[HtmlElement],
    bottomRight: Modifier[HtmlElement],
    farRight: Modifier[HtmlElement],
    linkMods: Option[Modifier[Anchor]] = None
)

object ListRow:

  given HtmlComponent[org.scalajs.dom.html.LI, ListRow] = (r: ListRow) => {
    def content: Modifier[HtmlElement] =
      Seq(
        cls := "block hover:bg-gray-50",
        div(
          cls := "px-4 py-4 sm:px-6 items-center flex",
          div(
            cls := "min-w-0 flex-1 pr-4",
            div(
              cls := "flex items-center justify-between",
              p(
                cls := "text-sm font-medium text-indigo-600 truncate",
                r.title
              ),
              div(
                cls := "ml-2 flex-shrink-0 flex",
                r.topRight
              )
            ),
            div(
              cls := "mt-2 sm:flex sm:justify-between",
              r.bottomLeft,
              r.bottomRight
            )
          ),
          r.farRight
        )
      )

    val c = content

    li(
      r.linkMods match
        case Some(m) => a(m, c)
        case _       => div(c)
    )
  }
