package works.iterative.ui
package scenarios

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import works.iterative.ui.components.ComponentContext

object Scenario:
  type Id = String

trait Scenario:
  trait ScenarioContext:
    def events: Observer[Any]

  def id: Scenario.Id

  def label: String

  def element(using ComponentContext[_]): Node

trait ScenarioExample:
  def title: String
  def element(using ComponentContext[_]): Node

object ScenarioExample:
  def apply(
      t: String,
      elem: ComponentContext[_] ?=> Node
  ): ScenarioExample =
    new ScenarioExample:
      override val title: String = t
      override def element(using ComponentContext[_]): Node = elem

trait ScenarioExamples:
  self: Scenario =>

  protected def examples(using
      ScenarioContext,
      ComponentContext[_]
  ): List[ScenarioExample]

  def example(name: String)(
      elem: ComponentContext[_] ?=> Node
  ): ScenarioExample =
    ScenarioExample(name, elem)

  override def element(using ComponentContext[_]): Node =
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
