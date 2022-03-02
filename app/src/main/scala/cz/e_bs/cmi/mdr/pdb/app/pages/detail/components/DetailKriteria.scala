package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import fiftyforms.ui.components.CustomAttrs
import fiftyforms.ui.components.Icons
import java.time.LocalDate
import fiftyforms.ui.components.Color

object DetailKriteria:
  case class ViewModel(
      nazev: String,
      kapitola: String,
      bod: String,
      status: String,
      statusColor: Color,
      splneno: Boolean,
      dukazy: List[DukazKriteria.ViewModel],
      container: HtmlElement = div()
  ) {
    val id = s"${kapitola}${bod}"
  }

  def apply($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "pb-5 border-b border-gray-200",
      h3(
        cls := "text-xl leading-6 font-bold text-gray-900",
        child.text <-- $m.map(_.nazev)
      ),
      h3(
        cls := "mt-2 max-w-4xl text-sm text-gray-500",
        "Kapitola ",
        child.text <-- $m.map(_.kapitola),
        " bod ",
        child <-- $m.map(_.bod)
      )
    )
