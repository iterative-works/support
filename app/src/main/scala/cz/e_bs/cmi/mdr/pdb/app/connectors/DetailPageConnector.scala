package cz.e_bs.cmi.mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.UserInfo
import pages.detail.DetailPage
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.components.DetailOsoby
import cz.e_bs.cmi.mdr.pdb.Parameter
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.components.SeznamParametru
import cz.e_bs.cmi.mdr.pdb.app.components.Color

object DetailPageConnector {
  trait AppState {
    def details: EventStream[UserInfo]
    def parameters: EventStream[List[Parameter]]
    def actionBus: Observer[Action]
  }
}

case class DetailPageConnector(state: DetailPageConnector.AppState)(
    $page: Signal[Page.Detail]
)(using router: Router[Page]):
  val $oscChangeSignal = $page.splitOne(_.osobniCislo)((osc, _, _) => osc)
  val $pageChangeSignal =
    $oscChangeSignal.flatMap(osc =>
      EventStream.fromSeq(Seq(FetchUserDetails(osc), FetchParameters(osc)))
    )
  // TODO: filter the value based on the current osc
  // OSC change will fetch new data, but still
  // - we need to be sure that what we got is really what we ought to display
  // - we want to display stale data accordingly (at least with loading indicator)
  val $data = state.details.startWithNone
  val $params = state.parameters.startWithNone

  def render: HtmlElement =
    AppPage().render(
      $data.combineWithFn($params)(_ zip _)
        .map(_.map(buildModel))
        .split(_ => ())((_, _, s) => DetailPage.render(s)),
      $pageChangeSignal --> state.actionBus
    )

  private def buildModel(
      o: UserInfo,
      p: List[Parameter]
  ): DetailPage.ViewModel =
    DetailPage.ViewModel(
      DetailOsoby.ViewModel(
        o.personalNumber,
        o.name,
        o.email,
        o.phone,
        o.img,
        None,
        None
      ),
      p.map { param =>
        SeznamParametru.Parametr(
          id = param.name,
          nazev = param.name,
          status = "Nesplněno",
          statusColor = Color.red
        )
      }
    )
