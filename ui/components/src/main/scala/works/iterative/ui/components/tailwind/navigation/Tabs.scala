package works.iterative
package ui.components.tailwind.navigation

import com.raquo.laminar.api.L.{*, given}
import works.iterative.core.MessageId
import works.iterative.ui.components.tailwind.ComponentContext

object Tabs:
  def apply[T](tabs: Seq[(MessageId, T)], selected: Signal[MessageId])(
      updates: Observer[T]
  )(using
      ctx: ComponentContext
  ): HtmlElement =
    val m = tabs
      .map { case (t, v) =>
        t.toString -> v
      }
      .to(Map)
      .withDefault(_ => tabs.head._2)

    div(
      div(
        cls := "sm:hidden",
        label(forId := "tabs", cls := "sr-only", "Select a tab"),
        select(
          idAttr := "tabs",
          name := "tabs",
          cls := "block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md",
          tabs.map { case (t, _) =>
            option(
              defaultSelected <-- selected.map(t == _),
              value := t.toString,
              ctx.messages(t).getOrElse(t.toString)
            )
          },
          onChange.mapToValue.map(m(_)) --> updates
        )
      ),
      div(
        cls := "hidden sm:block",
        div(
          cls := "border-b border-gray-200",
          nav(
            cls := "-mb-px flex space-x-8",
            aria.label := "Tabs",
            tabs.map { case (t, v) =>
              a(
                href := "#",
                cls := "whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm",
                cls <-- selected.map(s =>
                  if t == s then "border-indigo-500 text-indigo-600 "
                  else
                    "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                ),
                ctx.messages(t).getOrElse(t.toString),
                onClick.preventDefault.mapTo(v) --> updates
              )
            }
          )
        )
      )
    )
