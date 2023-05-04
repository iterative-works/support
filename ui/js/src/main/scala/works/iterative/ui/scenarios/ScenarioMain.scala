package works.iterative.ui.scenarios

import scala.scalajs.js.annotation.{JSExportTopLevel, JSImport}
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import scala.scalajs.js
import works.iterative.ui.JsonMessageCatalogue
import works.iterative.core.MessageCatalogue
import works.iterative.ui.components.tailwind.ComponentContext
import works.iterative.ui.components.tailwind.StyleGuide
import ui.components.tailwind.TailwindSupport
import com.raquo.waypoint.*

import scala.scalajs.js.Dictionary

trait ScenarioMain(
    prefix: String,
    scenarios: List[Scenario],
    messages: js.Any,
    css: js.Any
) extends TailwindSupport:

  val scenarioMap: Map[Scenario.Id, Scenario] =
    scenarios.map(s => (s.id, s)).toMap

  val messageCatalogue: MessageCatalogue = new JsonMessageCatalogue:
    override val messages: Dictionary[String] =
      ScenarioMain.this.messages.asInstanceOf[js.Dictionary[String]]

  val scenarioRoute: Route[Scenario.Id, String] =
    Route.onlyFragment[Scenario.Id, String](
      identity[String],
      identity[String],
      pattern = root / prefix / "index.html" withFragment fragment[String]
    )

  given router: Router[Scenario.Id] = Router[Scenario.Id](
    routes = List(scenarioRoute),
    identity[String],
    identity[String],
    identity[String],
    routeFallback = _ => scenarios.head.id
  )(
    windowEvents(_.onPopState),
    unsafeWindowOwner
  )

  def main(args: Array[String]): Unit =
    given MessageCatalogue = messageCatalogue

    given ComponentContext with
      val messages: MessageCatalogue = messageCatalogue
      val style: StyleGuide = StyleGuide.default

    def container: HtmlElement =
      div(
        cls("h-full"),
        div(
          cls(
            "fixed inset-y-0 z-50 flex w-72 flex-col"
          ),
          div(
            cls(
              "flex grow flex-col gap-y-5 overflow-y-auto border-r border-gray-200 bg-white px-6 pb-4"
            ),
            navTag(
              cls("flex flex-1 flex-col"),
              ul(
                role("list"),
                cls("flex flex-1 flex-col gap-y-7"),
                children <-- router.currentPageSignal.map(id =>
                  scenarios.map(s =>
                    li(
                      a(
                        href(s"#${s.id}"),
                        cls(
                          "group flex gap-x-3 rounded-md p-2 text-sm leading-6 font-semibold"
                        ),
                        if s.id == id then cls("bg-gray-50 text-indigo-600")
                        else
                          cls(
                            "text-gray-700 hover:text-indigo-600 hover:bg-gray-50"
                          )
                        ,
                        s.label
                      )
                    )
                  )
                )
              )
            )
          )
        ),
        mainTag(
          cls("h-full pl-72"),
          div(
            cls(
              "h-full max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8"
            ),
            child <-- router.currentPageSignal.map(scenarioMap(_).element)
          )
        )
      )

    val appContainer = dom.document.querySelector("#app")
    render(appContainer, container)
