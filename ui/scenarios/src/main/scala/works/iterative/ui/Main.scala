package works.iterative.ui

import works.iterative.ui.scenarios.ScenarioMain

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportTopLevel, JSImport}

@js.native
@JSImport("stylesheets/main.css?inline", JSImport.Default)
object Css extends js.Any

@js.native
@JSImport("resources/messages.json", JSImport.Default)
object Messages extends js.Any

@JSExportTopLevel("SupportScenarioMain", "main")
object Main
    extends ScenarioMain(
      "ui",
      List(ComboBoxScenarioModule),
      Messages,
      Css
    )
