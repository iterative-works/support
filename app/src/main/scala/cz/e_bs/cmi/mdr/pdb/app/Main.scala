package cz.e_bs.cmi.mdr.pdb.app

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import scala.scalajs.js.Date
import com.raquo.waypoint.Router
import com.raquo.waypoint.SplitRender
import cz.e_bs.cmi.mdr.pdb.app.services.DataFetcher
import scala.scalajs.js.JSON
import zio.json._
import cz.e_bs.cmi.mdr.pdb.UserInfo
import cz.e_bs.cmi.mdr.pdb.app.state.AppState

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
          .render
      )
      .collectStatic(Page.Dashboard)(connectors.DashboardPageConnector().render)
      .collect[Page.NotFound](pg =>
        pages.errors.NotFoundPage(Routes.homePage, pg.url)
      )
      .collect[Page.UnhandledError](pg =>
        pages.errors
          .UnhandledErrorPage(
            Routes.homePage,
            pg.errorName,
            pg.errorMessage
          )
      )
      .collectStatic(Page.Directory)(
        connectors
          .DirectoryPageConnector(state.users, state.actionBus)
          .render
      )
    div(child <-- pageSplitter.$view)

  // Pull in the stylesheet
  val css: Css.type = Css
