package cz.e_bs.cmi.mdr.pdb.app.pages.detail

import com.raquo.laminar.api.L.{*, given}

import components._

object DetailPage:

  case class ViewModel(
      osoba: DetailOsoby.ViewModel,
      parametry: SeznamParametru.ViewModel
  )

  def render($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8",
      div(
        cls := "flex flex-col space-y-4",
        DetailOsoby.render($m.map(_.osoba)),
        SeznamParametru.render($m.map(_.parametry))
      )
    )
