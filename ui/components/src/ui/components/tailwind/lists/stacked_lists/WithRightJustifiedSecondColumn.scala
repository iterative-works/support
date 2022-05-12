package works.iterative
package ui
package components.tailwind
package lists.stacked_lists

import com.raquo.laminar.api.L.{*, given}

import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.{UList, LI, Paragraph}
import works.iterative.ui.model.AppEvent
import model.{ListItem, ItemList, Label, ItemProp, HighlightColor}

object WithRightJustifiedSecondColumn:
  given Icons.IconComponent("h-5 w-5")
  def leftProp(i: Int): HtmlComponent[Paragraph, ItemProp] =
    (prop: ItemProp) =>
      p(
        cls(
          "mt-2 flex items-center text-sm text-gray-500 sm:mt-0"
        ),
        cls(
          if prop.icon.isDefined then "sm:ml-6"
          else if i != 0 then "sm:ml-2"
          else ""
        ),
        prop.icon.map(_.element),
        prop.text.toString
      )

  val rightProp: HtmlComponent[org.scalajs.dom.html.Div, ItemProp] =
    (prop: ItemProp) =>
      div(
        cls("mt-2 flex items-center text-sm text-gray-500 sm:mt-0"),
        prop.icon.map(_.element),
        prop.text.toString
      )

  given HtmlComponent[Paragraph, Label] =
    (l: Label) =>
      p(
        cls := "px-2 inline-flex text-xs leading-5 font-semibold rounded-full",
        // TODO: macros
        // cls(colorClass(color)),
        cls := (l.color match {
          case HighlightColor.Red    => "text-red-800 bg-red-100"
          case HighlightColor.Amber  => "text-amber-800 bg-amber-100"
          case HighlightColor.Green  => "text-green-800 bg-green-100"
          case HighlightColor.Yellow => "text-yellow-800 bg-yellow-100"
          case HighlightColor.Orange => "text-orange-800 bg-orange-100"
          case HighlightColor.Gray   => "text-gray-800 bg-gray-100"
        }),
        l.text
      )

  val defaultItem: ComponentContext ?=> HtmlComponent[LI, ListItem] =
    (i: ListItem) =>
      li(
        a(
          href := i.href,
          onClick.preventDefault.mapTo(AppEvent.NavigateTo(i.href)) --> summon[
            ComponentContext
          ].eventBus,
          cls := "block hover:bg-gray-50",
          div(
            cls := "px-4 py-4 sm:px-6 items-center flex",
            div(
              cls := "min-w-0 flex-1 pr-4",
              div(
                cls := "flex items-center justify-between",
                p(
                  cls := "text-sm font-medium text-indigo-600 truncate",
                  i.title.toString
                ),
                div(
                  cls := "ml-2 flex-shrink-0 flex",
                  i.label.map(_.element)
                )
              ),
              div(
                cls := "mt-2 sm:flex sm:justify-between",
                div(
                  cls("sm:flex"),
                  i.leftProps.zipWithIndex.map { case (p, i) =>
                    leftProp(i).render(p)
                  }
                ),
                i.rightProp.map(rightProp.render(_))
              )
            )
            // r.farRight
          )
        )
      )

  def defaultList(using
      HtmlComponent[LI, ListItem]
  ): ComponentContext ?=> HtmlComponent[org.scalajs.dom.html.Div, ItemList] =
    (l: ItemList) =>
      div(
        cls := "bg-white shadow overflow-hidden sm:rounded-md",
        l.items.map(i => {
          div(
            cls("relative"),
            h3(
              cls(
                "z-10 sticky top-0 border-t border-b border-gray-200 bg-gray-50 px-6 py-1 text-sm font-medium text-gray-500"
              ),
              i.title.toString
            ),
            ul(
              role := "list",
              cls := "divide-y divide-gray-200",
              cls("relative"),
              i.items.map(_.element)
            )
          )
        })
      )
