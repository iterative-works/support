package works.iterative.forms.scenarios

import zio.http.*
import zio.http.template.*
import portaly.forms.*
import zio.json.*
import portaly.forms.service.impl.rest.FormPersistenceCodecs.given
import java.nio.file.Paths

object FormCustomElementScenario extends Scenario:

    override val id = "formCustomElement"

    override val label = "Form Custom Element"

    private val formDescriptor: portaly.forms.Form = portaly.forms.Form("test", "0.1")(
        Field("hello"),
        Field("world")
    )

    private val assetsDir =
        Paths.get(
            "..",
            "js",
            "target",
            s"scala-${works.iterative.forms.scenarios.BuildInfo.scalaVersion}",
            "forms-scenarios-fastopt"
        ).toRealPath().toAbsolutePath()

    override val content =
        iframe(srcAttr := s"/scenarios/${id}/page", widthAttr := "100%", heightAttr := "100%")

    val page =
        Html.raw(s"""
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Basic Form Element Test</title>
            <!-- Tailwind CSS -->
            <script src="https://cdn.tailwindcss.com"></script>
            <script src="https://unpkg.com/htmx.org@2.0.2"></script>
            <!-- Assets dir: ${assetsDir} -->
            <script src="/assets/main.js"></script>
        </head>
        <body>
            <h1>Form rendered below</h1>
            <iw-form form-id="test" entity="abc" src="/form"></iw-form>
            <script>Main.main()</script>
        </body>
        </html>
        """)

    override val routes = Routes(
        Method.GET / Root / "form" -> handler(Response.json(formDescriptor.toJson)),
        Method.GET / Root / "scenarios" / string("scenario-id") / "page" -> handler(
            (scenarioId: String, _: Request) =>
                Response.html(page)
        )
    ) @@ Middleware.serveDirectory(
        Path.empty / "assets",
        assetsDir.toFile()
    )
end FormCustomElementScenario
