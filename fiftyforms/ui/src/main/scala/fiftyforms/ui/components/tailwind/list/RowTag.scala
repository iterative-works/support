package fiftyforms.ui.components.tailwind
package list

import com.raquo.laminar.api.L.{*, given}

object RowTag:
  case class ViewModel(text: String, color: Color)
  def render($m: Signal[ViewModel]): HtmlElement =
    inline def colorClass(color: Color): Seq[String] =
      import ColorWeight._
      List(color.bg(w100), color.text(w800))

    p(
      cls := "px-2 inline-flex text-xs leading-5 font-semibold rounded-full",
      cls <-- $m.map(t => colorClass(t.color)),
      child.text <-- $m.map(_.text)
    )
