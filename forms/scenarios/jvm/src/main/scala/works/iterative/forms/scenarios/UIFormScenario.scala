package works.iterative.forms.scenarios

import zio.http.template.*

object UIFormScenario extends Scenario:
    val id = "uiForm"

    val label = "UI Form"

    override def page: Html = "Hello UI Form!"
end UIFormScenario
