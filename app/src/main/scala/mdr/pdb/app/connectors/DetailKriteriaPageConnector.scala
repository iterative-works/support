package mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import mdr.pdb.parameters.*
import mdr.pdb.users.query.UserInfo
import mdr.pdb.app.pages.detail.DetailParametruPage
import pages.detail.DetailKriteriaPage
import pages.detail.components.DukazyKriteria
import com.raquo.waypoint.Router
import mdr.pdb.app.components.AppPage
import mdr.pdb.parameters.*

object DetailKriteriaPageConnector {
  trait AppState extends AppPage.AppState {
    def details: EventStream[UserInfo]
    def parameters: EventStream[List[Parameter]]
  }
}

case class DetailKriteriaPageConnector(
    state: DetailKriteriaPageConnector.AppState
)(
    $page: Signal[Page.DetailKriteria]
)(using Router[Page]):
  val $paramChangeSignal =
    $page.splitOne(p =>
      (p.osobniCislo.value, p.parametr.value, p.kriterium.value)
    )((x, _, _) => x)
  val $pageChangeSignal =
    $paramChangeSignal.map(
      FetchParameterCriteria(_, _, _, Page.DetailKriteria(_, _, _))
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
    AppPage(state)(
      $merged.map(_.map(buildModel))
        .split(_ => ())((_, s, $s) =>
          DetailKriteriaPage($s)(state.actionBus.contramap {
            case DukazyKriteria.Add =>
              NavigateTo(
                Page.UpravDukazKriteria(
                  Page.Titled(s.osoba.osobniCislo, Some(s.osoba.jmeno)),
                  Page.Titled(s.parametr.id, Some(s.parametr.nazev)),
                  Page.Titled(s.kriterium.id, Some(s.kriterium.id))
                )
              )
          })
        ),
      $pageChangeSignal --> state.actionBus
    )

  private def buildModel(
      o: UserInfo,
      p: Parameter,
      k: ParameterCriteria
  ): DetailKriteriaPage.ViewModel =
    DetailKriteriaPage.ViewModel(
      o.toDetailOsoby,
      p.toParametr(_ => a()),
      k.toKriterium(_ => a())
    )
