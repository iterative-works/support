package works.iterative.forms.scenarios

import zio.*
import zio.http.*
import zio.http.template.*

object ScenarioServer extends ZIOAppDefault:
    val view = ScenarioView

    val scenarios = List(FormCustomElementScenario, UIFormScenario)

    val routes: Routes[Any, Nothing] = Routes(
        Method.GET / Root -> handler(Response.html(
            view.layout(scenarios, Scenario.empty)
        )),
        Method.GET / Root / "scenarios" / string("scenario-id") -> handler(
            (scenarioId: String, _: Request) =>
                Response.html(view.layout(
                    scenarios,
                    scenarios.find(_.id == scenarioId).getOrElse(Scenario.empty)
                ))
        )
    ) ++ scenarios.map(_.routes).reduceLeft(_ ++ _)

    override def run = Server.serve(routes).provide(Server.default)
end ScenarioServer
