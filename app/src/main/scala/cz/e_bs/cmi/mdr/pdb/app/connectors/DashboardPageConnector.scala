package cz.e_bs.cmi.mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.pages.dashboard.DashboardPage
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage

class DashboardPageConnector(using router: Router[Page]):
  def render: HtmlElement =
    AppPage.render(Val(Some(DashboardPage.render)))
