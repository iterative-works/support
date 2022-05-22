package works.iterative
package ui.components.tailwind.lists.grid_lists

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.Color
import works.iterative.ui.components.tailwind.ColorWeight

object SimpleCards:

  case class Initials(
      text: String,
      color: Color,
      weight: ColorWeight = ColorWeight.w600
  ):
    def element: HtmlElement =
      div(
        cls := "flex items-center justify-center w-16 text-white text-sm font-medium rounded-l-md",
        cls(color.bg(weight)),
        text
      )

  case class IconButton(icon: SvgElement, screenReaderText: String):
    def element: HtmlElement =
      button(
        tpe := "button",
        cls := "w-8 h-8 bg-white inline-flex items-center justify-center text-gray-400 rounded-full bg-transparent hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
        span(cls := "sr-only", screenReaderText),
        icon
      )

  case class CardTitleLink(text: String, link: String):
    def element(
        clicked: Option[Observer[Unit]] = None,
        classes: String = "text-gray-900 font-meduim hover:text-gray-600"
    ): HtmlElement =
      a(
        href(link),
        clicked.map(onClick.mapTo(()) --> _),
        cls(classes),
        text
      )

  case class CardTitle(text: String):
    def element(
        classes: String = "text-gray-900 font-meduim hover:text-gray-600"
    ): HtmlElement =
      div(cls(classes), text)

  case class CardBody(
      title: HtmlElement,
      subtitle: String,
      button: Option[Button]
  ):
    def element: HtmlElement = div(
      cls := "flex items-center justify-between border-t border-r border-b border-gray-200 bg-white rounded-r-md truncate",
      div(
        cls := "flex-1 px-4 py-2 text-sm truncate",
        title,
        p(cls := "text-gray-500", subtitle)
      ),
      div(cls := "flex-shrink-0 pr-2", button)
    )

  case class Card(initials: Initials, body: CardBody):
    def element: HtmlElement =
      div(
        cls := "col-span-1 flex shadow-sm rounded-md",
        initials.element.amend(cls("flex-shrink-0")),
        body.element.amend(cls("flex-1"))
      )

  case class LinkCard(
      initials: Initials,
      body: CardBody,
      link: String
  ):
    def element(clicked: Observer[Unit]): HtmlElement = element(Some(clicked))
    def element(clicked: Option[Observer[Unit]] = None): HtmlElement =
      a(
        cls("block"),
        href(link),
        clicked.map(onClick.mapTo(()) --> _),
        Card(initials, body).element
      )

  case class ListHeader(text: String):
    def element(
        classes: String =
          "text-gray-500 text-xs font-medium uppercase tracking-wide"
    ): HtmlElement = h2(cls(classes), text)

  case class SimpleList(header: Option[HtmlElement], cards: Seq[HtmlElement]):
    def element(
        gap: String = "gap-5 sm:gap-6",
        cols: String = "grid-cols-1 sm:grid-cols-2 lg:grid-cols-4"
    ): HtmlElement =
      div(
        header,
        ul(
          role := "list",
          cls := "mt-3 grid",
          cls(gap),
          cls(cols),
          cards.map(li(_))
        )
      )
