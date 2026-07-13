// PURPOSE: Shared HTML shell and response helpers for the SSR form scenarios
// PURPOSE: Explicit utf-8 charset and a single doctype so fragment swaps and patches stay intact

package works.iterative.forms.scenarios

import scalatags.Text.all.*
import scalatags.Text.tags2
import zio.http.Response

object ScenarioHtml:

    private val style = """
        body { font-family: sans-serif; max-width: 40rem; margin: 2rem auto; }
        .field { margin: 0.5rem 0; }
        .field label { display: block; font-weight: bold; }
        .field input, .field textarea, .field select { width: 100%; box-sizing: border-box; }
        .required { color: #b00; margin-left: 0.2rem; }
        .field-errors { color: #b00; font-size: 0.9rem; }
        section { border-left: 3px solid #ddd; padding-left: 1rem; margin: 1rem 0; }
    """

    val htmxScript: Frag = script(src := "https://unpkg.com/htmx.org@2.0.2")

    // The version the transport bindings target; newer tags renamed the patch events
    val datastarScript: Frag = script(
        src := "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.2/bundles/datastar.js",
        `type` := "module"
    )

    def shell(pageTitle: String, scripts: Frag, inner: Frag): String =
        "<!doctype html>" + html(
            head(
                meta(charset := "utf-8"),
                tags2.title(pageTitle),
                scripts,
                tag("style")(raw(style))
            ),
            body(inner)
        ).render

    // Response.html would prepend its own doctype, breaking fragment swaps.
    // Explicit utf-8 charset: browsers otherwise submit forms in Latin-1
    def htmlResponse(content: String): Response =
        Response(
            body = zio.http.Body.fromString(content),
            headers = zio.http.Headers(zio.http.Header.ContentType(
                zio.http.MediaType.text.html,
                charset = Some(java.nio.charset.StandardCharsets.UTF_8)
            ))
        )
end ScenarioHtml
