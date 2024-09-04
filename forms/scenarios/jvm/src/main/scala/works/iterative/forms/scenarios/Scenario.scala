package works.iterative.forms.scenarios

import zio.http.*
import zio.http.template.*

trait Scenario:
    def id: String
    def label: String
    def content: Html
    def routes: Routes[Any, Nothing] = Routes.empty
end Scenario

object Scenario:
    val empty = new Scenario:
        override val id = ""
        override val label = ""
        override val content =
            div(
                idAttr := "scenario",
                classAttr("text-center"),
                Dom.raw("""<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="mx-auto h-12 w-12 text-gray-400">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 0 1 0 1.972l-11.54 6.347a1.125 1.125 0 0 1-1.667-.986V5.653Z" />
                    </svg>
                    """),
                h3(
                    classAttr("mt-2 text-sm font-semibold text-gray-900"),
                    "No scenario selected"
                ),
                p(
                    classAttr("mt-1 text-sm text-gray-500"),
                    "Get started by selecting a scenario from the sidebar."
                )
            )

    end empty
end Scenario
