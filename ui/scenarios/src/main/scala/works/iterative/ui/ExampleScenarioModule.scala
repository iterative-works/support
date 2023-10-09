package works.iterative.ui

import com.raquo.laminar.api.L.*
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.scenarios.Scenario.Id
import works.iterative.ui.scenarios.{Scenario, ScenarioExample, ScenarioExamples}

object ExampleScenarioModule extends Scenario with ScenarioExamples:

  override val id: Id = "example"

  override val label: String = "Example"

  override protected def examples(using
      ScenarioContext,
      ComponentContext[?]
  ): List[ScenarioExample] =
    List(simple)

  private def simple: ScenarioExample = example("ready")(
    div("Hello World!")
  )
