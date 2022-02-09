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

  val base =
    js.`import`.meta.env.BASE_URL
      .asInstanceOf[String]
      .init // Drop the ending slash

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
