// PURPOSE: Scenario proving the server-side HTML form loop: GET renders, POST validates and re-renders
// PURPOSE: Exercises conditions, repeated add/remove and declared validation without any client-side app

package works.iterative.forms.scenarios

import zio.http.{Response, Routes, Method, Root, Request, handler}
import zio.http.template.Html
import scalatags.Text.all.*
import works.iterative.forms.*
import works.iterative.core.MessageCatalogue
import works.iterative.scenarios.Scenario
import works.iterative.ui.model.forms.FormState

object SsrFormScenario extends Scenario:
    val id = "ssrForm"
    val label = "SSR Form"

    val formDeclaration: Form = InquiryFormLoop.formDeclaration

    given MessageCatalogue = InquiryProofForm.messages

    private val postAction = s"/$id/form"

    val initialState: FormData = InquiryFormLoop.initialState

    private val renderer =
        UIFormHtmlRenderer(InquiryFormLoop.displayResolver, FormTransport.htmx)
    private val builder = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))

    def renderFormTag(form: Form, state: FormState, validation: FormValidationState): Tag =
        renderer.render(builder.buildForm(form, state, validation, None), postAction)

    private def shell(inner: Frag): String =
        ScenarioHtml.shell("SSR Form", ScenarioHtml.htmxScript, inner)

    private def htmlResponse(content: String): Response = ScenarioHtml.htmlResponse(content)

    override def page: Html =
        Html.raw(shell(renderFormTag(formDeclaration, initialState, FormValidationState.valid)))

    def respond(raw: Map[String, Seq[String]], hxRequest: Boolean): Response =
        InquiryFormLoop.transition(raw) match
            case InquiryFormLoop.Outcome.Render(state, validation) =>
                respondForm(state, hxRequest, validation)
            case InquiryFormLoop.Outcome.Submitted(data) =>
                htmlResponse(shell(InquiryFormLoop.receivedFrag(data)))

    private def respondForm(
        state: FormData,
        hxRequest: Boolean,
        validation: FormValidationState
    ): Response =
        val formTag = renderFormTag(formDeclaration, state, validation)
        if hxRequest then htmlResponse(formTag.render)
        else htmlResponse(shell(formTag))
    end respondForm

    override val routes: Routes[Any, Nothing] = Routes(
        Method.GET / Root / id / "page" -> handler(
            htmlResponse(shell(renderFormTag(
                formDeclaration,
                initialState,
                FormValidationState.valid
            )))
        ),
        Method.POST / Root / id / "form" -> handler { (req: Request) =>
            // Body.asURLEncodedForm merges duplicate field names into one comma-joined
            // value, corrupting multi-value fields like __items; QueryParams.decode
            // preserves duplicates and percent-encoded commas
            req.body.asString.map { bodyString =>
                val raw = zio.http.QueryParams.decode(bodyString)
                    .map.view.mapValues(_.toSeq).toMap
                respond(raw, req.headers.get("HX-Request").contains("true"))
            }.orDie
        }
    )
end SsrFormScenario
