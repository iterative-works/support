package works.iterative.ui.components.tailwind
package list

import com.raquo.laminar.api.L.*

object RowTag:
  def apply(text: String, color: Color): HtmlElement =
    /*
    inline def colorClass(color: Color): Seq[String] =
      import ColorWeight._
      List(color.bg(w100), color.text(w800))
     */

    p(
      cls := "px-2 inline-flex text-xs leading-5 font-semibold rounded-full",
      // cls(colorClass(color)),
      cls := (color match {
        case Color.red    => "text-red-800 bg-red-100"
        case Color.amber  => "text-amber-800 bg-amber-100"
        case Color.orange => "text-orange-800 bg-orange-100"
        case Color.yellow => "text-yellow-800 bg-yellow-100"
        case Color.green  => "text-green-800 bg-green-100"
        case _            => "text-gray-800 bg-gray-100"
      }),
      text
    )
