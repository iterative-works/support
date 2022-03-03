package mdr.pdb.app

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import scala.scalajs.js.Date
import com.raquo.waypoint.Router
import com.raquo.waypoint.SplitRender
import mdr.pdb.app.services.DataFetcher
import scala.scalajs.js.JSON
import zio.json._
import mdr.pdb.UserInfo
import mdr.pdb.app.state.AppState

@js.native
@JSImport("stylesheets/main.css", JSImport.Namespace)
object Css extends js.Any

@js.native
@JSImport("data/users.json", JSImport.Default)
object mockUsers extends js.Object

@js.native
@JSImport("params/pdb-params.json", JSImport.Default)
object pdbParams extends js.Object

@JSExportTopLevel("app")
object Main:

  @JSExport
  def main(args: Array[String]): Unit =
    import Routes.given
    onLoad {
      setupAirstream()
      val appContainer = dom.document.querySelector("#app")
      val _ =
        render(
          appContainer,
          renderPage(state.MockAppState(using unsafeWindowOwner, router))
        )
    }

  private def onLoad(f: => Unit): Unit =
    documentEvents.onDomContentLoaded.foreach(_ => f)(unsafeWindowOwner)

  private def setupAirstream()(using router: Router[Page]): Unit =
    AirstreamError.registerUnhandledErrorCallback(err =>
      router.forcePage(
        Page.UnhandledError(
          Some(err.getClass.getName), // TODO: Fill only in dev mode
          Some(err.getMessage)
        )
      )
    )

  def renderPage(state: AppState)(using
      router: Router[Page]
  ): HtmlElement =
    val pageSplitter = SplitRender[Page, HtmlElement](router.$currentPage)
      .collectSignal[Page.Detail](
        connectors
          .DetailPageConnector(state)(_)
          .apply
      )
      .collectSignal[Page.DetailParametru](
        connectors
          .DetailParametruPageConnector(state)(_)
          .apply
      )
      .collectSignal[Page.DetailKriteria](
        connectors
          .DetailKriteriaPageConnector(state)(_)
          .apply
      )
      .collectSignal[Page.UpravDukazKriteria](
        pages.detail.UpravDukaz.Connector(state)(_).apply
      )
      .collectStatic(Page.Dashboard)(
        connectors.DashboardPageConnector(state).apply
      )
      .collect[Page.NotFound](pg =>
        pages.errors.NotFoundPage(Routes.homePage, pg.url, state.actionBus)
      )
      .collect[Page.UnhandledError](pg =>
        pages.errors
          .UnhandledErrorPage(
            pages.errors.UnhandledErrorPage
              .ViewModel(Routes.homePage, pg.errorName, pg.errorMessage),
            state.actionBus
          )
      )
      .collectStatic(Page.Directory)(
        connectors
          .DirectoryPageConnector(state)
          .apply
      )
    div(cls := "h-full", child <-- pageSplitter.$view)

  // Pull in the stylesheet
  val css: Css.type = Css
