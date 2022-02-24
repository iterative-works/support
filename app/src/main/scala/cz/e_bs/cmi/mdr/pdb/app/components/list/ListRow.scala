package cz.e_bs.cmi.mdr.pdb.app.components.list

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object ListRow:
  case class ViewModel(
      title: String,
      topRight: Modifier[HtmlElement],
      bottomLeft: Modifier[HtmlElement],
      bottomRight: Modifier[HtmlElement],
      farRight: Modifier[HtmlElement],
      containerElement: HtmlElement = div()
  )

  def apply($m: Signal[ViewModel]): HtmlElement =
    li(
      child <-- $m.map(m =>
        m.containerElement.amend(
          cls := "block hover:bg-gray-50",
          div(
            cls := "px-4 py-4 sm:px-6 items-center flex",
            div(
              cls := "min-w-0 flex-1 pr-4",
              div(
                cls := "flex items-center justify-between",
                p(
                  cls := "text-sm font-medium text-indigo-600 truncate",
                  m.title
                ),
                div(
                  cls := "ml-2 flex-shrink-0 flex",
                  m.topRight
                )
              ),
              div(
                cls := "mt-2 sm:flex sm:justify-between",
                m.bottomLeft,
                m.bottomRight
              )
            ),
            m.farRight
          )
        )
      )
    )
