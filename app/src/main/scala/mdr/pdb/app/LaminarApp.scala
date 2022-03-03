package mdr.pdb.app

import zio.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import org.scalajs.dom
import mdr.pdb.app.state.AppState
import com.raquo.waypoint.SplitRender

trait LaminarApp:
  def renderApp: Task[Unit]

object LaminarApp:
  def renderApp: RIO[LaminarApp, Unit] = ZIO.serviceWith(_.renderApp)

object LaminarAppLive:
  val layer: URLayer[Router[Page], LaminarApp] =
    (LaminarAppLive(_)).toLayer[LaminarApp]

class LaminarAppLive(router: Router[Page]) extends LaminarApp:
  given Router[Page] = router

  def renderApp: Task[Unit] =
    for
      _ <- setupAirstream
      _ <- renderLaminar
    yield ()

  private def renderLaminar: Task[Unit] =
    Task.attempt {
      val appContainer = dom.document.querySelector("#app")
      render(
        appContainer,
        renderPage(state.MockAppState(using unsafeWindowOwner, router))
      )
    }

  private def setupAirstream: Task[Unit] =
    Task.attempt {
      AirstreamError.registerUnhandledErrorCallback(err =>
        router.forcePage(
          Page.UnhandledError(
            Some(err.getClass.getName), // TODO: Fill only in dev mode
            Some(err.getMessage)
          )
        )
      )
    }

  private def renderPage(state: AppState)(using
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
