package cz.e_bs.cmi.mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.Parameter
import cz.e_bs.cmi.mdr.pdb.UserInfo
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.DetailParametruPage
import pages.detail.DetailParametruPage
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator

object DetailParametruPageConnector {
  trait AppState {
    def details: EventStream[UserInfo]
    def parameters: EventStream[List[Parameter]]
    def actionBus: Observer[Action]
  }
}

case class DetailParametruPageConnector(
    state: DetailParametruPageConnector.AppState
)(
    $page: Signal[Page.DetailParametru]
)(using router: Router[Page]):
  val $paramChangeSignal =
    $page.splitOne(p => (p.osobniCislo, p.idParametru))((x, _, _) => x)
  val $pageChangeSignal =
    $paramChangeSignal.map(FetchParameter(_, _))

  val $data = state.details.startWithNone
  val $params = state.parameters.startWithNone

  val $merged =
    $data.combineWithFn($params, $paramChangeSignal)((d, p, pc) =>
      for {
        da <- d
        pa <- p
        pb <- pa.find(_.id == pc._2)
      } yield (da, pb)
    )

  def render: HtmlElement =
    AppPage.render(
      $merged.map(_.map(buildModel))
        .split(_ => ())((_, _, s) => DetailParametruPage.render(s)),
      $pageChangeSignal --> state.actionBus
    )

  private def buildModel(
      o: UserInfo,
      p: Parameter
  ): DetailParametruPage.ViewModel =
    DetailParametruPage.ViewModel(
      o.toDetailOsoby,
      p.toParametr,
      p.criteria.map(
        _.toKriterium(c =>
          a(Navigator.navigateTo[Page](Page.DetailKriteria(o, p, c)))
        )
      )
    )
