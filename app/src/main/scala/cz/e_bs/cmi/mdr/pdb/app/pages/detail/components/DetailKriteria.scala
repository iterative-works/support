package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}

object DetailKriteria:
  type ViewModel = SeznamKriterii.Kriterium
  def render($m: Signal[ViewModel]): HtmlElement =
    div(
      h3(cls := "text-l font-bold text-gray-900"),
      child.text <-- $m.map(_.nazev)
    )
