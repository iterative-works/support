package cz.e_bs.cmi.mdr.pdb.app

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.components.{Navigation, Layout}
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
            "", // TODO: basePath
            Some(err.getClass.getName), // TODO: Fill only in dev mode
            Some(err.getMessage)
          )
        )
      )

      val _ = render(
        appContainer,
        Layout(
          logo,
          userProfile.signal,
          // TODO: make static, use user profile to filter
          allPages.signal,
          // TODO: make static, use user profile to filter
          userMenu.signal,
          renderPage
        )
      )
    }(unsafeWindowOwner)
  }

  def renderPage(using router: Router[Page]): HtmlElement =
    val pageSplitter = SplitRender[Page, HtmlElement](router.$currentPage)
      .collectSignal[Page.Detail](
        pages.DetailPage(osc =>
          EventStream.fromValue(ExampleData.persons.jmeistrova).delay(1000)
        )
      )
      .collectStatic(Page.Dashboard)(pages.DashboardPage)
      .collect[Page.NotFound](pg =>
        pages.errors.NotFoundPage(pg.url, pg.baseUrl)
      )
      .collect[Page.UnhandledError](pg =>
        pages.errors
          .UnhandledErrorPage(pg.baseUrl, pg.errorName, pg.errorMessage)
      )
      .collectStatic(Page.Directory)(
        pages.DirectoryPage(
          EventStream
            .fromValue(List(ExampleData.persons.jmeistrova))
        )
      )
    components.MainSection(child <-- pageSplitter.$view)

  // TODO: pages by logged in user
  val allPages = Var(List(Page.Directory, Page.Dashboard))

  val logo = Navigation.Logo(
    "Workflow",
    "https://tailwindui.com/img/logos/workflow-mark-indigo-300.svg"
  )

  // TODO: load user profile
  val userProfile = Var(
    UserProfile(
      "tom",
      UserInfo(
        "Tom Cook",
        "tom@example.com",
        "+420 222 866 180",
        None,
        "ČMI Medical",
        "ředitel"
      )
    )
  )

  // TODO: menu items by user profile
  val userMenu = Var(
    List(
      Navigation.MenuItem("Your Profile"),
      Navigation.MenuItem("Settings"),
      Navigation.MenuItem("Sign out")
    )
  )

  // Pull in the stylesheet
  val css: Css.type = Css
