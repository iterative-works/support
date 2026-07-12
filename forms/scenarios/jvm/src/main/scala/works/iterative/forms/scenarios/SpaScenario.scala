// PURPOSE: The client-side form loop for one declaration: serve it, host its custom element,
// PURPOSE: receive the posted FormR and echo the same dump shape as the SSR success page

package works.iterative.forms.scenarios

import zio.http.*
import zio.http.template.*
import works.iterative.forms.impl.FormR
import zio.json.*
import works.iterative.forms.service.impl.rest.FormPersistenceCodecs.given
import java.nio.file.Paths
import works.iterative.scenarios.Scenario

class SpaScenario(
    override val id: String,
    override val label: String,
    elementTag: String,
    formId: String,
    entity: String,
    declaration: works.iterative.forms.Form
) extends Scenario:

    // Mill fastLinkJS output for formsScenarios.js; the e2e and run tasks pass the linked
    // directory through the environment, the relative path covers ad-hoc launches from the
    // repository root
    private val assetsDir =
        sys.env.get("SCENARIOS_ASSETS")
            .map(Paths.get(_))
            .getOrElse(Paths.get("out", "formsScenarios", "js", "fastLinkJS.dest"))
            .toAbsolutePath()

    override val page =
        Html.raw(s"""
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$label</title>
            <!-- Assets dir: ${assetsDir} -->
            <script src="/${id}/assets/main.js"></script>
        </head>
        <body>
            <$elementTag form-id="$formId" entity="$entity" src="/${id}/form"></$elementTag>
            <script>RegisterElement.main()</script>
        </body>
        </html>
        """)

    /** Mirrors the SSR success dump: field paths to their submitted string values. */
    private def dump(data: FormR): Map[String, List[String]] =
        data.data.map((path, values) =>
            path.toHtmlName -> values.collect { case value: String => value }
        )

    override val routes = Routes(
        Method.GET / Root / id / "form" -> handler(
            Response.json(declaration.toJson)
        ),
        Method.GET / Root / id / "page" -> handler(
            Response.html(page)
        ),
        Method.POST / Root / id / "submit" -> handler { (req: Request) =>
            req.body.asString.map { body =>
                body.fromJson[FormR] match
                    case Right(data) => Response.json(dump(data).toJson)
                    case Left(error) => Response.badRequest(error)
            }.orDie
        }
    ) @@ Middleware.serveDirectory(
        Path.empty / id / "assets",
        assetsDir.toFile()
    )
end SpaScenario
