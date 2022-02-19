package cz.e_bs.cmi.mdr.pdb.app.pages.dashboard

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage
import cz.e_bs.cmi.mdr.pdb.app.Page

object DashboardPage:

  def render: HtmlElement =
    div("Dashboard page")
