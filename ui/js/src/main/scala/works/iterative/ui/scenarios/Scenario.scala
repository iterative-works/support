package works.iterative.ui
package scenarios

import com.raquo.laminar.api.L

import scala.scalajs.js.annotation.{JSExportTopLevel, JSImport}
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html

import scala.scalajs.js
import works.iterative.core.MessageCatalogue
import works.iterative.ui.components.tailwind.ComponentContext
import works.iterative.ui.components.tailwind.StyleGuide

object Scenario:
  type Id = String

trait Scenario:
  trait ScenarioContext:
    def events: Observer[Any]

  def id: Scenario.Id

  def label: String

  def element(using ComponentContext[_]): HtmlElement

trait ScenarioExample:
  def title: String
  def element(using ComponentContext[_]): HtmlElement

object ScenarioExample:
  def apply(
      t: String,
      elem: ComponentContext[_] ?=> HtmlElement
  ): ScenarioExample =
    new ScenarioExample:
      override val title: String = t
      override def element(using ComponentContext[_]): HtmlElement = elem

trait ScenarioExamples:
  self: Scenario =>

  protected def examples(using
      ScenarioContext,
      ComponentContext[_]
  ): List[ScenarioExample]

  override def element(using ComponentContext[_]): HtmlElement =
    val eventBus: EventBus[Any] = EventBus[Any]()

    given sc: ScenarioContext = new ScenarioContext:
      def events: Observer[Any] = eventBus.writer

    div(
      cls("flex flex-col space-y-5"),
      eventBus.events --> { e =>
        org.scalajs.dom.console.log(s"action: ${e.toString}")
      },
      examples.map(se => example(se.title, se.element))
    )

  def example(t: String, c: HtmlElement): Div =
    div(
      cls("bg-white overflow-hidden shadow rounded-lg"),
      div(
        cls("px-4 py-5 sm:p-6"),
        h3(
          cls("text-lg leading-6 font-medium text-gray-900 border-b"),
          t
        ),
        div(cls("px-5 py-5"), c)
      )
    )
