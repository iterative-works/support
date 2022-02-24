package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.Page
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.Action

object PageHeader:
  def apply(actionBus: Observer[Action])(using Router[Page]): HtmlElement =
    header(
      cls := "bg-white shadow-sm",
      div(
        cls := "max-w-7xl mx-auto py-4 px-4 sm:px-6 lg:px-8",
        h1(
          cls := "text-lg leading-6 font-semibold text-gray-900",
          Breadcrumbs(actionBus)
        )
      )
    )
