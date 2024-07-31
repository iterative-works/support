package works.iterative.ui.scenarios

import zio.*
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.*
import org.scalajs.dom
import works.iterative.ui.components.tailwind.TailwindSupport
import works.iterative.core.MessageCatalogue
import works.iterative.ui.JsonMessageCatalogue
import works.iterative.ui.components.ComponentContext

import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import works.iterative.ui.components.Modal
import works.iterative.ui.components.ZIODispatcher
import works.iterative.core.auth.UserProfile
import works.iterative.core.Language

trait ScenarioMain(
    prefix: String,
    scenarios: List[Scenario],
    messages: js.Any,
    @unused css: js.Any
) extends TailwindSupport:

    private val scenarioMap: Map[Scenario.Id, Scenario] =
        scenarios.map(s => (s.id, s)).toMap

    val messageCatalogue: MessageCatalogue = new JsonMessageCatalogue:
        override val language: Language = Language.CS
        override val messages: Dictionary[String] =
            ScenarioMain.this.messages.asInstanceOf[js.Dictionary[String]]

    private val scenarioRoute: Route[Scenario.Id, String] =
        Route.onlyFragment[Scenario.Id, String](
            identity[String],
            identity[String],
            pattern = root / prefix withFragment fragment[String]
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

    def main(@unused args: Array[String]): Unit =
        given ComponentContext[Any] with
            val currentUser: Signal[Option[UserProfile]] = Val(None)
            val messages: MessageCatalogue = messageCatalogue
            val modal: Modal = new Modal:
                override def open(content: HtmlElement): Unit = ()
                override def close(): Unit = ()
            val dispatcher: ZIODispatcher[Any] = ZIODispatcher.empty
            val runtime: Runtime[Any] = Runtime.default
        end given

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
        val _ = render(appContainer, container)
    end main
end ScenarioMain
