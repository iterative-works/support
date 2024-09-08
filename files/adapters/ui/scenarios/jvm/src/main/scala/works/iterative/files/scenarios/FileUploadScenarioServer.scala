package works.iterative.files.scenarios

import works.iterative.scenarios.Scenario
import zio.http.template.*

object FileUploadScenarioServer extends Scenario:
    override val id: String = "fileUpload"
    override val label: String = "File Upload"

    private val viteBase =
        "http://localhost:5173/target/scala-3.3.1/iw-support-files-ui-scenarios-fastopt"

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
                    s"${viteBase}/${this.id}.js"
                )
            ),
            // end if development
            title(this.label)
        ),
        body(
            span(classAttr("text-lg font-semibold"), s"${this.label}"),
            script(
                typeAttr("module"),
                s"import { scenario } from '${viteBase}/${this.id}.js'; scenario.main();"
            )
        )
    )
end FileUploadScenarioServer
