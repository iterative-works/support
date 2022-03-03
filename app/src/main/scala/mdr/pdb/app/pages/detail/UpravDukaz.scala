package mdr.pdb
package app
package pages.detail

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import mdr.pdb.app.components.AppPage
import mdr.pdb.app.pages.detail.components.UpravDukazForm
import fiftyforms.services.files.File

object UpravDukaz:

  type ThisPage = Page.UpravDukazKriteria
  type PageKey = (OsobniCislo, String, String)

  trait State {
    def details: EventStream[UserInfo]
    def parameters: EventStream[List[Parameter]]
    def availableFiles: EventStream[List[File]]
    def actionBus: Observer[Action]
  }

  def keyOfPage(page: ThisPage): PageKey =
    (page.osobniCislo.value, page.parametr.value, page.kriterium.value)

  def onChangeAction(key: PageKey): Action =
    FetchParameterCriteria(
      key._1,
      key._2,
      key._3,
      Page.UpravDukazKriteria(_, _, _)
    )

  class Connector(state: State)($page: Signal[ThisPage])(using Router[Page]):
    val $paramChangeSignal = $page.splitOne(keyOfPage)((x, _, _) => x)
    val $pageChangeSignal = $paramChangeSignal.map(onChangeAction)

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
        $merged.split(_ => ())((_, s, $s) =>
          PageComponent(
            $s.map(buildModel),
            state.availableFiles,
            state.actionBus.contramap {
              case UpravDukazForm.Cancelled =>
                NavigateTo(Page.DetailKriteria(s._1, s._2, s._3))
              case UpravDukazForm.AvailableFilesRequested =>
                FetchAvailableFiles(s._1.personalNumber)
            }
          )
        ),
        $pageChangeSignal --> state.actionBus
      )

    private def buildModel(
        o: UserInfo,
        p: Parameter,
        k: ParameterCriteria
    ): PageComponent.ViewModel =
      import connectors.*
      PageComponent.ViewModel(
        o.toDetailOsoby,
        p.toParametr(_ => a()),
        k.toKriterium(_ => a())
      )

  object PageComponent:
    import components.*

    case class ViewModel(
        osoba: DetailOsoby.ViewModel,
        parametr: DetailParametru.ViewModel,
        kriterium: DetailKriteria.ViewModel
    )

    def apply(
        $m: Signal[ViewModel],
        availableFilesStream: EventStream[List[File]],
        events: Observer[UpravDukazForm.Event]
    ): HtmlElement =
      div(
        cls := "max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8",
        div(
          cls := "flex flex-col space-y-4",
          div(
            DetailOsoby.header($m.map(_.osoba)),
            DetailParametru.header($m.map(_.parametr)).amend(cls := "mt-2")
          ),
          div(
            DetailKriteria($m.map(_.kriterium)),
            UpravDukazForm(availableFilesStream)(events)
          )
        )
      )
