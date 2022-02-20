package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}

object DetailParametru:
  type ViewModel = SeznamParametru.Parametr
  def render($m: Signal[ViewModel]): HtmlElement =
    div(
      h2(cls := "text-xl font-bold text-gray-900"),
      child.text <-- $m.map(_.nazev)
    )
