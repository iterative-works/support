package works.iterative.forms.scenarios

import zio.*
import zio.http.*
import zio.http.template.*
import zio.json.*
import portaly.forms.*
import portaly.forms.service.impl.rest.FormPersistenceCodecs.given
import java.nio.file.Paths

object ScenarioServer extends ZIOAppDefault:
    def formDescriptor: portaly.forms.Form = portaly.forms.Form("test", "0.1")(
        Field("hello"),
        Field("world")
    )

    val assetsDir =
        Paths.get(
            "..",
            "js",
            "target",
            s"scala-${works.iterative.forms.scenarios.BuildInfo.scalaVersion}",
            "forms-scenarios-fastopt"
        ).toRealPath().toAbsolutePath()

    val routes = Routes(
        Method.GET / Root -> handler(Response.html(Html.raw(s"""
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Basic Form Element Test</title>
                <!-- Tailwind CSS -->
                <script src="https://cdn.tailwindcss.com"></script>
                <!-- Assets dir: ${assetsDir} -->
                <script src="/assets/main.js"></script>
            </head>
            <body>
                <h1>Form rendered below</h1>
                <iw-form form-id="test" entity="abc" src="/form"></iw-form>
                <script>Main.main()</script>
            </body>
            </html>
            """))),
        Method.GET / Root / "form" -> handler(Response.json(formDescriptor.toJson))
    ) @@ Middleware.serveDirectory(
        Path.empty / "assets",
        assetsDir.toFile()
    )

    override def run = Server.serve(routes).provide(Server.default)
end ScenarioServer
