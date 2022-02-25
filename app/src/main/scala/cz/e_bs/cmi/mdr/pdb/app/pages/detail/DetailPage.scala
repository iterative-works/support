package cz.e_bs.cmi.mdr.pdb.app.pages.detail

import com.raquo.laminar.api.L.{*, given}

import components._
import cz.e_bs.cmi.mdr.pdb.app.Page
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.Action

object DetailPage:

  case class ViewModel(
      osoba: DetailOsoby.ViewModel,
      parametry: SeznamParametru.ViewModel
  )

  def apply($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8",
      div(
        cls := "flex flex-col space-y-4",
        DetailOsoby($m.map(_.osoba)),
        SeznamParametru($m.map(_.parametry))
      )
    )
