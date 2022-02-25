package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.components.CustomAttrs
import cz.e_bs.cmi.mdr.pdb.app.components.Icons
import java.time.LocalDate
import cz.e_bs.cmi.mdr.pdb.app.components.Color

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
      h3(
        cls := "text-l font-bold text-gray-900",
        child.text <-- $m.map(_.nazev)
      ),
      DukazyKriterii($m.map(_.dukazy))
    )
