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

@js.native
@JSImport("stylesheets/main.css", JSImport.Namespace)
object Css extends js.Any

@JSExportTopLevel("app")
object Main:

  @JSExport
  def main(args: Array[String]): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      given router: Router[Page] = Routes.router

      AirstreamError.registerUnhandledErrorCallback(err =>
        router.forcePage(
          Page.UnhandledError(
            Some(err.getClass.getName), // TODO: Fill only in dev mode
            Some(err.getMessage)
          )
        )
      )

      val _ = render(
        appContainer,
        renderPage
      )
    }(unsafeWindowOwner)
  }

  def renderPage(using router: Router[Page]): HtmlElement =
    val pageSplitter = SplitRender[Page, HtmlElement](router.$currentPage)
      .collectSignal[Page.Detail](
        pages
          .DetailPage(osc =>
            EventStream.fromValue(ExampleData.persons.jmeistrova).delay(1000)
          )(_)
          .render
      )
      .collectStatic(Page.Dashboard)(pages.DashboardPage().render)
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
        pages
          .DirectoryPage(
            EventStream
              .fromValue(List(ExampleData.persons.jmeistrova))
          )
          .render
      )
    div(child <-- pageSplitter.$view)

  // Pull in the stylesheet
  val css: Css.type = Css
