package cz.e_bs.cmi.mdr.pdb.app

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import scala.scalajs.js.Date

@js.native
@JSImport("stylesheets/main.css", JSImport.Namespace)
object Css extends js.Any

@JSExportTopLevel("app")
object Main:

  // Pull in the stylesheet
  val css: Css.type = Css

  @JSExport
  def main(args: Array[String]): Unit = {
    val appContainer = dom.document.querySelector("#app")
    val $time = EventStream.periodic(1000).mapTo(new Date().toTimeString)
    val appElement: Div = div(
      h1("Hello"),
      "Current time is: ",
      b(child.text <-- $time)
    )
    val pages = Var(List(Dashboard, Detail))
    val currentPage = Var(Dashboard)
    val root: RootNode =
      render(appContainer, Layout(pages.signal, currentPage.signal, appElement))
  }
