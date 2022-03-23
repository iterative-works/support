package ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}

object Switch {
  def apply(name: String, toggle: Var[Boolean]): HtmlElement =
    div(
      cls := "flex items-center",
      button(
        tpe := "button",
        cls := "relative inline-flex flex-shrink-0 h-6 w-11 border-2 border-transparent rounded-full cursor-pointer transition-colors ease-in-out duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
        cls <-- toggle.signal.map(a =>
          if a then "bg-indigo-600" else "bg-gray-200"
        ),
        role := "switch",
        dataAttr("aria-checked") := "false",
        dataAttr("aria-labelledby") := "active-only-label",
        span(
          dataAttr("aria-hidden") := "true",
          cls := "pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow transform ring-0 transition ease-in-out duration-200",
          cls <-- toggle.signal.map(a =>
            if a then "translate-x-5" else "translate-x-0"
          )
        ),
        composeEvents(onClick)(
          _.sample(toggle.signal).map(a => !a)
        ) --> toggle.writer
      ),
      span(
        cls := "ml-3",
        idAttr := "active-only-label",
        span(cls := "text-sm font-medium text-gray-900", name)
      )
    )
}
