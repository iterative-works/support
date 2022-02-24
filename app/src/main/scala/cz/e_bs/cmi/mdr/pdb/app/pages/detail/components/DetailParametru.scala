package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}

object DetailParametru:
  type ViewModel = SeznamParametru.Parametr
  def render($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "pb-5 border-b border-gray-200",
      h2(
        cls := "text-lg leading-6 font-medium text-gray-900",
        child.text <-- $m.map(_.nazev)
      ),
      p(
        cls := "mt-2 max-w-4xl text-sm text-gray-500",
        child.text <-- $m.map(_.popis)
      )
    )
