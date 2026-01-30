package works.iterative.ui

import com.raquo.laminar.api.L.*
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.scenarios.Scenario.Id
import works.iterative.ui.scenarios.{Scenario, ScenarioExample, ScenarioExamples}

object ComboBoxScenarioModule extends Scenario with ScenarioExamples:

    override val id: Id = "combo"

    override val label: String = "Combo"

    override protected def examples(using
        ScenarioContext,
        ComponentContext[?]
    ): List[ScenarioExample] =
        List(simple)

    private def simple: ScenarioExample = example("original combo") {
        import components.tailwind.form.ComboBox
        ComboBox(
            id = "combo",
            options = Val(
                List(
                    ComboBox.Option("One", active = false),
                    ComboBox.Option("Two", active = false),
                    ComboBox.Option("Three", active = false)
                )
            ),
            valueUpdates = Observer.empty
        ).toHtml
    }
end ComboBoxScenarioModule
