package works.iterative.files.scenarios

import zio.*
import zio.http.*
import zio.http.htmx.Attributes.PartialAttribute

// ZIO-http server app that serving the scenario
object ScenariosServer extends ZIOAppDefault:
    object Model:
        final case class ScenarioItem(id: ScenarioItem.Id, name: String, path: String)
        object ScenarioItem:
            opaque type Id = String
            object Id:
                def apply(value: String): Id = value
    end Model

    object View:
        object template extends zio.http.template.Attributes with zio.http.htmx.Attributes
            with zio.http.template.Elements:
            def roleAttr: PartialAttribute[String] = PartialAttribute("role")

        import template.*
        import Model.*

        // Applied partial layout from https://tailwindui.com/components/application-ui/application-shells/sidebar#component-5548358cb34897c6b28551f2ad885eec
        // Would need to add mobile responsivity

        def scenarios(items: NonEmptyChunk[ScenarioItem], selected: ScenarioItem.Id) =
            items.map: item =>
                val itemClass = if item.id == selected then "bg-gray-800 text-white"
                else "text-gray-400 hover:text-white hover:bg-gray-800"
                li(
                    a(
                        hrefAttr(s"?scenario=${item.id}"),
                        classAttr(
                            s"$itemClass group flex gap-x-3 rounded-md p-2 text-sm leading-6 font-semibold"
                        ),
                        item.name
                    )
                )

        def desktopSidebar(items: NonEmptyChunk[ScenarioItem], selected: ScenarioItem.Id) =
            div(
                classAttr(
                    "flex grow flex-col gap-y-5 overflow-y-auto bg-gray-900 px-6 pb-4"
                ),
                div(
                    classAttr("flex h-16 shrink-0 items-center"),
                    span(classAttr("text-white text-lg"), "Scenarios")
                ),
                nav(
                    classAttr("flex flex-1 flex-col"),
                    ul(
                        roleAttr("list"),
                        classAttr("flex flex-1 flex-col gap-y-7"),
                        li(
                            ul(
                                roleAttr("list"),
                                classAttr("-mx-2 space-y-1"),
                                scenarios(items, selected).toList
                            )
                        )
                    )
                )
            )

        def content(id: ScenarioItem.Id) = div(
            classAttr("relative"),
            iframe(
                classAttr("w-full overflow-hidden rounded-lg ring-1 ring-slate-900/10"),
                srcAttr(s"/scenarios/$id")
            )
        )

        def mainContent(selected: ScenarioItem.Id) = template.main(
            classAttr("py-10"),
            div(classAttr("px-4 sm:px-6 lg:px-8"), content(selected))
        )

        def index(items: NonEmptyChunk[ScenarioItem], selected: ScenarioItem.Id) = html(
            classAttr("h-full bg-white"),
            langAttr("en"),
            head(
                meta(charsetAttr("utf-8")),
                meta(nameAttr("viewport"), contentAttr("width=device-width, initial-scale=1")),
                script(srcAttr("https://cdn.tailwindcss.com")),
                script(
                    srcAttr("https://unpkg.com/htmx.org@1.9.12"),
                    PartialAttribute("integrity")(
                        "sha384-ujb1lZYygJmzgSwoxRggbCHcjc0rB2XoQrxeTUQyRjrOnlCoYta87iKBWq3EsdM2"
                    ),
                    PartialAttribute("crossorigin")("anonymous")
                ),
                title("Scenarios")
            ),
            body(
                classAttr("h-full"),
                // Static sidebar for desktop
                div(
                    classAttr("hidden lg:fixed lg:inset-y-0 lg:z-50 lg:flex lg:w-72 lg:flex-col"),
                    // Sidebar component
                    desktopSidebar(items, selected)
                ),
                div(classAttr("lg:pl-72"), mainContent(selected))
            )
        )

        def scenario(viteBase: String, item: ScenarioItem) = html(
            classAttr("h-full bg-white"),
            langAttr("en"),
            head(
                meta(charsetAttr("utf-8")),
                meta(nameAttr("viewport"), contentAttr("width=device-width, initial-scale=1")),
                script(srcAttr("https://cdn.tailwindcss.com")),
                // if development
                script(typeAttr("module"), srcAttr("http://localhost:5173/@vite/client")),
                script(
                    typeAttr("module"),
                    srcAttr(
                        s"${viteBase}/${item.path}"
                    )
                ),
                // end if development
                title(s"Scenario ${item.id}")
            ),
            body(
                span(classAttr("text-lg font-semibold"), s"Scenario ${item.id}"),
                script(
                    typeAttr("module"),
                    s"import { scenario } from '${viteBase}/${item.path}'; scenario.main();"
                )
            )
        )
    end View

    val scenarios = NonEmptyChunk(
        Model.ScenarioItem(Model.ScenarioItem.Id("1"), "Scenario 1", "scenario1.js")
    )

    val app: HttpApp[Any] =
        import codec.PathCodec.*
        Routes(
            Method.GET / empty -> handler((req: Request) =>
                Response.html(View.index(
                    scenarios,
                    req.queryParam("scenario").map(Model.ScenarioItem.Id(_)).getOrElse(
                        scenarios.head.id
                    )
                ))
            ),
            Method.GET / "scenarios" / string("scenario") -> handler((id: String, _: Request) =>
                val scenario = scenarios.find(_.id == Model.ScenarioItem.Id(id))
                scenario.fold(
                    Response.notFound(s"Scenario $id not found")
                )(item =>
                    Response.html(View.scenario(
                        "http://localhost:5173/target/scala-3.3.1/iw-support-files-ui-scenarios-fastopt",
                        item
                    ))
                )
            )
        ).toHttpApp
    end app

    def run = Server.serve(app).provide(Server.default)
end ScenariosServer
