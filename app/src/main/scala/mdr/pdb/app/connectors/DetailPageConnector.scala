package mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import mdr.pdb.UserInfo
import pages.detail.DetailPage
import mdr.pdb.app.components.AppPage
import mdr.pdb.app.components.PageLink
import mdr.pdb.app.pages.detail.components.DetailOsoby
import mdr.pdb.Parameter
import mdr.pdb.app.pages.detail.components.SeznamParametru
import fiftyforms.ui.components.tailwind.Color

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
  val $oscChangeSignal = $page.splitOne(_.osobniCislo.value)((osc, _, _) => osc)
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

  def apply: HtmlElement =
    AppPage(state.actionBus)(
      $data.combineWithFn($params)(_ zip _)
        .map(_.map(buildModel))
        .split(_ => ())((_, _, s) => DetailPage(s)),
      $pageChangeSignal --> state.actionBus
    )

  private def buildModel(
      o: UserInfo,
      p: List[Parameter]
  ): DetailPage.ViewModel =
    DetailPage.ViewModel(
      o.toDetailOsoby,
      p.map(
        _.toParametr(param =>
          PageLink.container(Page.DetailParametru(o, param), state.actionBus)
        )
      )
    )
