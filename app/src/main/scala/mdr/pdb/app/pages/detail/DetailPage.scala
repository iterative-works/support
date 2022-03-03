package mdr.pdb.app.pages.detail

import com.raquo.laminar.api.L.{*, given}

import components._
import mdr.pdb.app.Page
import com.raquo.waypoint.Router
import mdr.pdb.app.Action

object DetailPage:

  case class ViewModel(
      osoba: DetailOsoby.ViewModel,
      parametry: SeznamParametru.ViewModel
  )

  def apply($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "h-full overflow-y-auto max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8",
      div(
        cls := "flex flex-col space-y-4",
        DetailOsoby($m.map(_.osoba)),
        SeznamParametru($m.map(_.parametry))
      )
    )
