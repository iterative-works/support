// PURPOSE: Pins the Datastar form wiring — the exact data-on attributes that route submissions
// PURPOSE: and committed-choice change re-renders through @post actions with the form content type

package works.iterative.forms.datastar

import zio.test.*
import scalatags.Text.all.{Frag, span, stringFrag}
import works.iterative.core.{Language, MessageCatalogue}
import works.iterative.forms.*
import works.iterative.forms.impl.FormR
import works.iterative.ui.model.forms.*

object DatastarFormTransportSpec extends ZIOSpecDefault:

    given MessageCatalogue = MessageCatalogue.debug

    val noDisplay: DisplayResolver[FormState, Frag] = new DisplayResolver[FormState, Frag]:
        def resolve(id: IdPath, state: FormState)(using MessageCatalogue, Language): Frag =
            span("display:" + id.toHtmlName)

    def html(form: Form): String =
        val ui = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))
            .buildForm(form, FormR.empty, FormValidationState.valid, None)
        UIFormHtmlRenderer(noDisplay, DatastarFormTransport).render(ui, "/submit").render

    def spec = suite("DatastarFormTransport")(
        test("the form submits through a Datastar @post action with the form content type") {
            val out = html(Form("demo", "1")(Section("s")(Field("name"))))
            assertTrue(
                // Datastar prevents the native submission on form-level data-on:submit and
                // appends the submitter's name/value, so the __submit protocol rides unchanged
                out.contains(
                    """data-on:submit="@post('/submit', {contentType: 'form'})""""
                ),
                // the plain POST contract stays for no-JS degradation, server-side validation
                out.contains("""method="post""""),
                out.contains("""action="/submit""""),
                out.contains("novalidate"),
                !out.contains("hx-post")
            )
        },
        test("only committed-choice controls trigger a change re-render, same list as HTMX") {
            val out = html(Form("demo", "1")(Section("s")(Field("name"))))
            val selectors = FormTransport.rerenderSelectors.mkString(", ")
            val expected =
                s"""evt.target.matches(&quot;$selectors&quot;) &amp;&amp; """ +
                    "@post('/submit', {contentType: 'form'})"
            assertTrue(out.contains(s"""data-on:change="$expected""""))
        },
        test("the request header constant matches what Datastar sends") {
            assertTrue(DatastarFormTransport.requestHeader == ("datastar-request" -> "true"))
        }
    )
end DatastarFormTransportSpec
