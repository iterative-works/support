package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import fiftyforms.ui.components.tailwind.Icons

object DukazyKriteria:
  sealed trait Action
  case object Add extends Action

  type ViewModel = List[DukazKriteria.ViewModel]

  def apply($m: Signal[ViewModel])(actions: Observer[Action]): HtmlElement =
    div(
      child.maybe <-- $m.map(m =>
        if (m.isEmpty) then Some(prazdnyDukaz(actions))
        else None
      ),
      children <-- $m.map(_.zipWithIndex).split(_._2)((_, _, $s) =>
        DukazKriteria($s.map(_._1))
      )
    )

  private def prazdnyDukaz(actions: Observer[Action]): HtmlElement =
    button(
      tpe := "button",
      onClick.preventDefault.mapTo(Add) --> actions,
      cls := "relative block w-full border-2 border-gray-300 border-dashed rounded-lg p-12 text-center hover:border-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
      Icons.outline
        .`document-add`(12)
        .amend(svg.cls := "mx-auto text-gray-400"),
      span(
        cls := "mt-2 block text-sm font-medium text-gray-900",
        "Přidat důkaz"
      )
    )
