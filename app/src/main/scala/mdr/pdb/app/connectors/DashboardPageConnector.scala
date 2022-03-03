package mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import mdr.pdb.app.pages.dashboard.DashboardPage
import com.raquo.waypoint.Router
import mdr.pdb.app.components.AppPage

class DashboardPageConnector(actionBus: Observer[Action])(using
    router: Router[Page]
):
  def apply: HtmlElement =
    AppPage(actionBus)(Val(Some(DashboardPage.render)))
