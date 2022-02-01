package cz.e_bs.cmi.mdr.pdb

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

@JSExportTopLevel("app")
object Main {

  @JSExport
  def main(args: Array[String]): Unit = {
    val appContainer = dom.document.querySelector("#app")
    val appElement: Div = div(
      h1("Hello"),
      "Current time is:",
      b("12:00")
    )
    val root: RootNode = render(appContainer, appElement)
  }
}
