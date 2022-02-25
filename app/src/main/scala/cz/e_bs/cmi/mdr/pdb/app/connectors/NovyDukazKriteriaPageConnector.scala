package cz.e_bs.cmi.mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.Parameter
import cz.e_bs.cmi.mdr.pdb.UserInfo
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.DetailParametruPage
import pages.detail.DetailKriteriaPage
import pages.detail.NovyDukazKriteriaPage
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage
import cz.e_bs.cmi.mdr.pdb.ParameterCriteria

// TODO: extract common pieces for all detail kriteria pages
object NovyDukazKriteriaPageConnector {
  trait AppState {
    def details: EventStream[UserInfo]
    def parameters: EventStream[List[Parameter]]
    def actionBus: Observer[Action]
  }
}

case class NovyDukazKriteriaPageConnector(
    state: NovyDukazKriteriaPageConnector.AppState
)(
    $page: Signal[Page.NovyDukazKriteria]
)(using Router[Page]):
  val $paramChangeSignal =
    $page.splitOne(p =>
      (p.osobniCislo.value, p.parametr.value, p.kriterium.value)
    )((x, _, _) => x)
  val $pageChangeSignal =
    $paramChangeSignal.map(
      FetchParameterCriteria(_, _, _, Page.NovyDukazKriteria(_, _, _))
    )

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

  def apply: HtmlElement =
    AppPage(state.actionBus)(
      $merged.map(_.map(buildModel))
        .split(_ => ())((_, _, $s) => NovyDukazKriteriaPage($s)),
      $pageChangeSignal --> state.actionBus
    )

  private def buildModel(
      o: UserInfo,
      p: Parameter,
      k: ParameterCriteria
  ): NovyDukazKriteriaPage.ViewModel =
    NovyDukazKriteriaPage.ViewModel(
      o.toDetailOsoby,
      p.toParametr(_ => a()),
      k.toKriterium(_ => a())
    )
