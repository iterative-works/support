package fiftyforms.ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}

object Display:

  enum Breakpoint:
    case sm, md, lg, xl, `2xl`

  enum DisplayClass:
    case block, `inline-block`, `inline`, flex, `inline-flex`, table,
    `inline-table`, `table-caption`

  object ShowUpFrom:
    inline def apply(
        br: Breakpoint,
        dc: DisplayClass = DisplayClass.block
    ): HtmlElement =
      div(
        cls := "hidden",
        cls := s"${br}:${dc}"
      )

  object HideUpTo:
    inline def apply(br: Breakpoint): HtmlElement =
      div(
        cls := s"${br}:hidden"
      )
