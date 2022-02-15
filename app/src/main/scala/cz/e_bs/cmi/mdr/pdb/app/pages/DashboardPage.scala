package cz.e_bs.cmi.mdr.pdb.app.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage
import cz.e_bs.cmi.mdr.pdb.app.Page

class DashboardPage(using router: Router[Page]) extends AppPage:
  override def pageContent: HtmlElement =
    div("Dashboard page")
