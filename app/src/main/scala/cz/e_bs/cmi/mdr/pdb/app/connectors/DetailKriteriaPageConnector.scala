package cz.e_bs.cmi.mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.Parameter
import cz.e_bs.cmi.mdr.pdb.UserInfo
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.DetailParametruPage
import pages.detail.DetailKriteriaPage
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage
import cz.e_bs.cmi.mdr.pdb.ParameterCriteria
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator

object DetailKriteriaPageConnector {
  trait AppState {
    def details: EventStream[UserInfo]
    def parameters: EventStream[List[Parameter]]
    def actionBus: Observer[Action]
  }
}

case class DetailKriteriaPageConnector(
    state: DetailKriteriaPageConnector.AppState
)(
    $page: Signal[Page.DetailKriteria]
)(using Router[Page]):
  val $paramChangeSignal =
    $page.splitOne(p => (p.osobniCislo, p.idParametru, p.idKriteria))(
      (x, _, _) => x
    )
  val $pageChangeSignal =
    $paramChangeSignal.map(FetchParameterCriteria(_, _, _))

  val $data = state.details.startWithNone
  val $params = state.parameters.startWithNone

  val $merged =
    $data.combineWithFn($params, $paramChangeSignal)((d, p, pc) =>
      for {
        da <- d
        pa <- p
        pb <- pa.find(_.id == pc._2)
        ka <- pb.criteria.find(_.id == pc._3)
      } yield (da, pb, ka)
    )

  def render: HtmlElement =
    AppPage().render(
      $merged.map(_.map(buildModel))
        .split(_ => ())((_, _, s) => DetailKriteriaPage.render(s)),
      $pageChangeSignal --> state.actionBus
    )

  private def buildModel(
      o: UserInfo,
      p: Parameter,
      k: ParameterCriteria
  ): DetailKriteriaPage.ViewModel =
    DetailKriteriaPage.ViewModel(
      o.toDetailOsoby,
      p.toParametr,
      k.toKriterium()
    )