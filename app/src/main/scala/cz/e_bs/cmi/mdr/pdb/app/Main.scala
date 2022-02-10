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
        pages.DetailPage(
          new DataFetcher[String, Osoba] {
            override def fetch(osobniCislo: String, o: Observer[Osoba]): Unit =
              o.delay(1000).onNext(ExampleData.persons.jmeistrova)
          }
        )
      )
      .collectStatic(Page.Dashboard)(pages.DashboardPage)
      .collectStatic(Page.Directory)(pages.DirectoryPage)
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
