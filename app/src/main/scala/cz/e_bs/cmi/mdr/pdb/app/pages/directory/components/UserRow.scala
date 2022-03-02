package cz.e_bs.cmi.mdr.pdb.app.pages.directory.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.airstream.core.Signal
import fiftyforms.ui.components.Avatar

object UserRow:
  case class ViewModel(
      osobniCislo: String,
      celeJmeno: String,
      prijmeni: String,
      hlavniFunkce: Option[String],
      img: Option[String],
      container: () => HtmlElement = () => div()
  ) {
    val search = prijmeni.toLowerCase
  }

  def apply($m: Signal[ViewModel]): HtmlElement =
    inline def avatarImage =
      Avatar($m.map(_.img)).avatarImage(10)

    li(
      div(
        cls := "bg-white relative px-6 py-5 flex items-center space-x-3 hover:bg-gray-50 focus-within:ring-2 focus-within:ring-inset focus-within:ring-pink-500",
        div(
          cls := "flex-shrink-0",
          child <-- avatarImage
        ),
        div(
          cls := "flex-1 min-w-0",
          child <-- $m.map { o =>
            o.container()
              .amend(
                cls := "focus:outline-none",
                span(
                  cls := "absolute inset-0",
                  aria.hidden := true
                ),
                p(
                  cls := "text-sm font-medium text-gray-900",
                  o.celeJmeno
                ),
                p(
                  cls := "text-sm text-gray-500 truncate",
                  o.hlavniFunkce
                )
              )
          }
        )
      )
    )
