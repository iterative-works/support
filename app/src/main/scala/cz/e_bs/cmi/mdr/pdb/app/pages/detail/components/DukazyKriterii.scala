package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.components.Icons

object DukazyKriterii:
  type ViewModel = List[DukazKriteria.ViewModel]
  def apply($m: Signal[ViewModel]): HtmlElement =
    div(
      child.maybe <-- $m.map(m =>
        if (m.isEmpty) then Some(prazdnyDukaz) else None
      ),
      children <-- $m.map(_.zipWithIndex).split(_._2)((_, _, $s) =>
        DukazKriteria($s.map(_._1))
      )
    )

  private def prazdnyDukaz: HtmlElement =
    button(
      tpe := "button",
      cls := "relative block w-full border-2 border-gray-300 border-dashed rounded-lg p-12 text-center hover:border-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
      Icons.outline
        .`document-add`(12)
        .amend(svg.cls := "mx-auto text-gray-400"),
      span(
        cls := "mt-2 block text-sm font-medium text-gray-900",
        "Přidat důkaz"
      )
    )
