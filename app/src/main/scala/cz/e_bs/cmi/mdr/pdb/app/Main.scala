package cz.e_bs.cmi.mdr.pdb.app

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.*
import cz.e_bs.cmi.mdr.pdb.app.components.{Navigation, Layout}
import zio.json.{*, given}

import scala.scalajs.js.Date

@js.native
@JSImport("stylesheets/main.css", JSImport.Namespace)
object Css extends js.Any

@JSExportTopLevel("app")
object Main:

  @JSExport
  def main(args: Array[String]): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      val _ = render(
        appContainer,
        Layout(
          logo,
          userProfile.signal,
          // TODO: make static, use user profile to filter
          allPages.signal,
          // TODO: make static, use user profile to filter
          userMenu.signal,
          renderPage(router.$currentPage)
        )(using router)
      )
    }(unsafeWindowOwner)
  }

  given JsonEncoder[Page] = DeriveJsonEncoder.gen[Page]
  given JsonDecoder[Page] = DeriveJsonDecoder.gen[Page]

  val base = "/mdr/pdb"

  val router = Router[Page](
    routes = List(
      Route.static(Page.Dashboard, root / "dashboard", basePath = base),
      Route.static(Page.Detail, root / "detail", basePath = base)
    ),
    serializePage = _.toJson,
    deserializePage = _.fromJson[Page]
      .fold(s => throw IllegalStateException(s), identity),
    getPageTitle = _.title,
    routeFallback = _ => Page.Dashboard,
    deserializeFallback = _ => Page.Dashboard
  )(
    $popStateEvent = windowEvents.onPopState,
    owner = unsafeWindowOwner
  )

  def renderPage($currentPage: Signal[Page]): HtmlElement =
    val pageSplitter = SplitRender[Page, HtmlElement]($currentPage)
      .collectStatic(Page.Detail)(pages.DetailPage)
      .collectStatic(Page.Dashboard)(pages.DashboardPage)
    components.MainSection(child <-- pageSplitter.$view)

  val $time = EventStream.periodic(1000).mapTo(new Date().toTimeString)

  def appElement: Div = div(
    h1("Hello"),
    "Current time is: ",
    b(child.text <-- $time)
  )

  // TODO: pages by logged in user
  val allPages = Var(List(Page.Dashboard, Page.Detail))
  // TODO: page routing
  val currentPage = Var(Page.Dashboard)

  val logo = Navigation.Logo(
    "Workflow",
    "https://tailwindui.com/img/logos/workflow-mark-indigo-300.svg"
  )

  // TODO: load user profile
  val userProfile = Var(
    UserProfile(
      "Tom Cook",
      "tom@example.com",
      "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80"
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
