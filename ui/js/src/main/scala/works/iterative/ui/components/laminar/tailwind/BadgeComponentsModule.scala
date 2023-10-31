package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L.*
import works.iterative.ui.components.laminar.tailwind.color.ColorKind

trait BadgeComponentsModule:

  object badges:
    def pill(name: String, color: ColorKind): HtmlElement =
      p(
        cls(
          "px-2 inline-flex text-xs leading-5 font-semibold rounded-full"
        ),
        color(800).text,
        color(100).bg,
        name
      )

    def pill(name: String, color: Signal[ColorKind]): HtmlElement =
      p(
        cls(
          "px-2 inline-flex text-xs leading-5 font-semibold rounded-full"
        ),
        cls <-- color.map(_.apply(800).text.toCSS),
        cls <-- color.map(_.apply(100).bg.toCSS),
        name
      )
