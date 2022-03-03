package mdr.pdb.app.pages.dashboard

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import mdr.pdb.app.components.AppPage
import mdr.pdb.app.Page

object DashboardPage:

  def render: HtmlElement =
    div("Dashboard page")
