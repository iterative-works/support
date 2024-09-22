package works.iterative.ui
package scenarios

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import works.iterative.ui.components.ComponentContext

object Scenario:
    type Id = String

trait Scenario:
    trait ScenarioContext:
        def events: Observer[Any]

    def id: Scenario.Id

    def label: String

    def element(using ComponentContext[?]): Node
end Scenario

trait ScenarioExample:
    def title: String
    def element(using ComponentContext[?]): Node

object ScenarioExample:
    def apply(
        t: String,
        elem: ComponentContext[?] ?=> Node
    ): ScenarioExample =
        new ScenarioExample:
            override val title: String = t
            override def element(using ComponentContext[?]): Node = elem
end ScenarioExample

trait ScenarioExamples:
    self: Scenario =>

    protected def examples(using
        ScenarioContext,
        ComponentContext[?]
    ): List[ScenarioExample]

    def example(name: String)(
        elem: ComponentContext[?] ?=> Node
    ): ScenarioExample =
        ScenarioExample(name, elem)

    override def element(using ComponentContext[?]): Node =
        val eventBus: EventBus[Any] = EventBus[Any]()

        given sc: ScenarioContext = new ScenarioContext:
            def events: Observer[Any] = eventBus.writer

        div(
            cls("flex flex-col space-y-5"),
            eventBus.events --> { e =>
                org.scalajs.dom.console.log(s"action: ${e.toString}")
            },
            examples.map(se => renderExample(se.title, se.element))
        )
    end element

    private def renderExample(t: String, c: Node): Div =
        div(
            cls("bg-white overflow-visible shadow rounded-lg"),
            div(
                cls("px-4 py-5 sm:p-6"),
                h3(
                    cls("text-lg leading-6 font-medium text-gray-900 border-b"),
                    t
                ),
                div(cls("px-5 py-5"), c)
            )
        )
end ScenarioExamples
