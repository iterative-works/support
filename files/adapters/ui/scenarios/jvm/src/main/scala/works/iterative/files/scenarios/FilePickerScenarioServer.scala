package works.iterative.files.scenarios

import works.iterative.scenarios.Scenario
import zio.*
import zio.http.*
import zio.http.template.*
import java.nio.file.Paths

object FilePickerScenarioServer extends Scenario:
    override val id: String = "filePicker"
    override val label: String = "File Picker"

    // TODO: get from config or somewhere
    private val viteBase =
        "http://localhost:5173/target/scala-3.5.0/files-ui-scenarios-fastopt"

    override val page = html(
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
                    s"${viteBase}/scenario${this.id}.js"
                )
            ),
            // end if development
            title(this.label)
        ),
        body(
            span(classAttr("text-lg font-semibold"), s"${this.label}"),
            script(
                typeAttr("module"),
                s"import { scenario } from '${viteBase}/scenario${this.id}.js'; scenario.main();"
            ),
            div(idAttr := "app")
        )
    )

    private val assetsDir =
        Paths.get(
            "..",
            "js",
            "public",
            "assets"
        ).toRealPath().toAbsolutePath()

    override def routes: Routes[Any, Nothing] =
        (super.routes ++ Routes(
            Method.GET / Root / id / "files.json" -> handler((req: Request) =>
                ZIO.scoped:
                    ZIO.readURLInputStream(getClass.getResource("/sample_files.json")).debug
                        .flatMap(_.readAll(4096))
                        .map(content => Response.json(content.asString))
                        .catchAll: err =>
                            ZIO.succeed:
                                Response.internalServerError(err.toString())
            )
        )) @@ Middleware.serveDirectory(
            Path.empty / "assets",
            assetsDir.toFile()
        )
end FilePickerScenarioServer
