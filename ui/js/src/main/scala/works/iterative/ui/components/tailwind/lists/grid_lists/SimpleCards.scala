package works.iterative
package ui.components.tailwind.lists.grid_lists

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.Color
import works.iterative.ui.components.tailwind.ColorWeight
import works.iterative.ui.components.headless.Items
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom

object SimpleCards:

  case class Item(
      initials: HtmlElement,
      body: HtmlElement
  )

  def initials(
      text: String,
      color: Color,
      weight: ColorWeight = ColorWeight.w600
  ): HtmlElement =
    div(
      cls := "flex-shrink-0 flex items-center justify-center w-16 text-white text-sm font-medium rounded-l-md",
      cls(color.toCSSWithColorWeight("bg", weight)),
      text
    )

  def iconButton(icon: SvgElement, screenReaderText: String): Button =
    button(
      tpe := "button",
      cls := "w-8 h-8 bg-white inline-flex items-center justify-center text-gray-400 rounded-full bg-transparent hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
      span(cls := "sr-only", screenReaderText),
      icon
    )

  def titleLink(
      clicked: Option[Observer[Unit]] = None,
      classes: String = "text-gray-900 font-medium hover:text-gray-600 truncate"
  )(text: String, link: String): HtmlElement =
    a(
      href(link),
      clicked.map(onClick.mapTo(()) --> _),
      cls(classes),
      text
    )

  def title(
      classes: String = "text-gray-900 font-medium hover:text-gray-600 truncate"
  )(text: String): HtmlElement =
    div(cls(classes), text)

  def body(
      title: HtmlElement,
      subtitle: HtmlElement,
      button: Option[Button] = None
  ): HtmlElement = div(
    cls := "flex-1 flex items-center justify-between border-t border-r border-b border-gray-200 bg-white rounded-r-md truncate",
    div(
      cls := "flex-1 px-4 py-2 text-sm truncate",
      title,
      p(cls := "text-gray-500", subtitle)
    ),
    div(cls := "flex-shrink-0 pr-2", button)
  )

  private def item(i: Item): HtmlElement =
    div(
      cls := "col-span-1 flex shadow-sm rounded-md",
      i.initials,
      i.body
    )

  def header(
      text: String,
      classes: String =
        "text-gray-500 text-xs font-medium uppercase tracking-wide"
  )(content: HtmlElement): HtmlElement = div(h2(cls(classes), text), content)

  def frame(
      gap: String = "gap-5 sm:gap-6",
      cols: String = "grid-cols-1 sm:grid-cols-2 lg:grid-cols-4"
  )(el: ReactiveHtmlElement[dom.html.UList]): HtmlElement =
    el.amend(cls := "mt-3 grid", cls(gap), cls(cols))

  def apply[A](f: A => Item): Items[A] =
    Items(frame(), item).contramap(f)

  case class LinkProps(href: String, events: Option[Observer[Unit]] = None)

  def linked[A](l: A => LinkProps)(
      f: A => Item
  ): Items[A] =
    apply(f).map(i =>
      card => {
        val lp = l(i)
        a(
          cls("block"),
          href(lp.href),
          lp.events.map(onClick.preventDefault.mapTo(()) --> _),
          card
        )
      }
    )
