package works.iterative.forms.scenarios

import zio.http.template.*
import works.iterative.scenarios.Scenario

object UIFormScenario extends Scenario:
    val id = "uiForm"

    val label = "UI Form"

    override def page: Html = "Hello UI Form!"
end UIFormScenario
