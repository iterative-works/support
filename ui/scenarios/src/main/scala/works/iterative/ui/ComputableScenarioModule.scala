package works.iterative.ui

import com.raquo.laminar.api.L.*
import works.iterative.ui.components.laminar.ComputableComponent
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.model.Computable
import works.iterative.ui.scenarios.Scenario.Id
import works.iterative.ui.scenarios.{Scenario, ScenarioExample, ScenarioExamples}

object ComputableScenarioModule extends Scenario with ScenarioExamples:

  override val id: Id = "computable"

  override val label: String = "Computable"

  override protected def examples(using
      ScenarioContext,
      ComponentContext[?]
  ): List[ScenarioExample] =
    List(simple)

  private def simple: ScenarioExample = example("ready")(
    ComputableComponent(div)(
      Val(Computable.Ready(span("Hello World!")))
    ).element
  )
