package works.iterative.ui.components.tailwind
package list

import com.raquo.laminar.api.L.{*, given}

object RowTag:
  def apply(text: String, color: Color): HtmlElement =
    inline def colorClass(color: Color): Seq[String] =
      import ColorWeight._
      List(color.bg(w100), color.text(w800))

    p(
      cls := "px-2 inline-flex text-xs leading-5 font-semibold rounded-full",
      cls(colorClass(color)),
      text
    )
