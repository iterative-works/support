package cz.e_bs.cmi.mdr.pdb.app.pages.detail

import com.raquo.laminar.api.L.{*, given}

import components._

object DetailParametruPage:

  case class ViewModel(
      osoba: DetailOsoby.ViewModel,
      parametr: DetailParametru.ViewModel,
      kriteria: SeznamKriterii.ViewModel
  )

  def render($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "h-full overflow-y-auto max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8",
      div(
        cls := "flex flex-col space-y-4",
        DetailOsoby.header($m.map(_.osoba)),
        DetailParametru($m.map(_.parametr)),
        SeznamKriterii($m.map(_.kriteria))
      )
    )
