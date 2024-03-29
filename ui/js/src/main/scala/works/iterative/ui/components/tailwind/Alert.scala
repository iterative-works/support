package works.iterative
package ui.components.tailwind

import com.raquo.laminar.api.L.*

object Alert:
  enum Kind(val color: Color, val icon: String => SvgElement):
    case Error extends Kind(Color.red, Icons.solid.`x-circle`(_))
    case Warning extends Kind(Color.yellow, Icons.solid.exclamation(_))
    case Success extends Kind(Color.green, Icons.solid.`check-circle`(_))

  def success(title: String | HtmlElement) = Alert(Kind.Success, title)

  def success(title: String | HtmlElement, content: String | HtmlElement) =
    Alert(Kind.Success, title, Some(content))

  def warning(title: String | HtmlElement) = Alert(Kind.Warning, title)

  def warning(title: String | HtmlElement, content: String | HtmlElement) =
    Alert(Kind.Warning, title, Some(content))

  def error(title: String | HtmlElement) = Alert(Kind.Error, title)

  def error(title: String | HtmlElement, content: String | HtmlElement) =
    Alert(Kind.Error, title, Some(content))

  given Conversion[Alert, HtmlElement] = _.element

import Alert.*

case class Alert(
    kind: Kind,
    title: String | HtmlElement,
    content: Option[String | HtmlElement] = None
):
  def element =
    div(
      cls := "rounded-md p-4",
      cls(kind.color.bg(ColorWeight.w50)),
      div(
        cls := "flex",
        div(
          cls := "flex-shrink-0",
          kind.icon(s"h-5 w-5 ${kind.color.text(ColorWeight.w400)}")
        ),
        div(
          cls := "ml-3",
          h3(
            cls := "text-sm font-medium",
            cls(kind.color.text(ColorWeight.w800)),
            title match
              case t: String      => t
              case e: HtmlElement => e
          ),
          content.map(c =>
            div(
              cls := "mt-2 text-sm",
              cls(kind.color.text(ColorWeight.w700)),
              c match
                case t: String      => p(t)
                case e: HtmlElement => e
            )
          )
        )
      )
    )
