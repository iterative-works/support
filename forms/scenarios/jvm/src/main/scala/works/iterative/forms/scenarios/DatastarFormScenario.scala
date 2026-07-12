// PURPOSE: Scenario proving the Datastar form loop: GET renders, actions answer patch-elements SSE
// PURPOSE: Same InquiryFormLoop as the HTMX scenario — parity is the point, only the transport differs

package works.iterative.forms.scenarios

import zio.http.{Body, Header, Headers, MediaType, Method, Request, Response, Root, Routes, handler}
import zio.http.template.Html
import scalatags.Text.all.*
import works.iterative.forms.*
import works.iterative.forms.datastar.DatastarFormTransport
import works.iterative.core.MessageCatalogue
import works.iterative.scalatags.datastar.sse.ServerSentEvents
import works.iterative.scenarios.Scenario
import works.iterative.ui.model.forms.FormState

object DatastarFormScenario extends Scenario:
    val id = "datastarForm"
    val label = "Datastar Form"

    val formDeclaration: Form = InquiryFormLoop.formDeclaration

    given MessageCatalogue = InquiryProofForm.messages

    private val postAction = s"/$id/form"

    val initialState: FormData = InquiryFormLoop.initialState

    private val renderer =
        UIFormHtmlRenderer(InquiryFormLoop.displayResolver, DatastarFormTransport)
    private val builder = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))

    def renderFormTag(state: FormState, validation: FormValidationState): Tag =
        renderer.render(builder.buildForm(formDeclaration, state, validation, None), postAction)

    // The morph targets elements by id; the received dump rides the form's own id to replace it
    private val formHtmlId: String =
        builder.buildForm(formDeclaration, initialState, FormValidationState.valid, None)
            .id.toHtmlId

    private def shell(inner: Frag): String =
        ScenarioHtml.shell(label, ScenarioHtml.datastarScript, inner)

    override def page: Html =
        Html.raw(shell(renderFormTag(initialState, FormValidationState.valid)))

    /** One or more rendered SSE events as the action response Datastar reads and applies. */
    private def sseResponse(events: String*): Response =
        Response(
            body = Body.fromString(events.mkString),
            headers = Headers(Header.ContentType(
                MediaType.text.`event-stream`,
                charset = Some(java.nio.charset.StandardCharsets.UTF_8)
            ))
        )

    def respond(raw: Map[String, Seq[String]], datastarRequest: Boolean): Response =
        InquiryFormLoop.transition(raw) match
            case InquiryFormLoop.Outcome.Render(state, validation) =>
                val formTag = renderFormTag(state, validation)
                if datastarRequest then
                    sseResponse(ServerSentEvents.patchElements(formTag))
                else ScenarioHtml.htmlResponse(shell(formTag))
            case InquiryFormLoop.Outcome.Submitted(data) =>
                val received = InquiryFormLoop.receivedFrag(data)
                if datastarRequest then
                    sseResponse(ServerSentEvents.patchElements(
                        div(scalatags.Text.all.id := formHtmlId)(received)
                    ))
                else ScenarioHtml.htmlResponse(shell(received))
                end if

    override val routes: Routes[Any, Nothing] = Routes(
        Method.GET / Root / id / "page" -> handler(
            ScenarioHtml.htmlResponse(shell(renderFormTag(
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
                val (header, expected) = DatastarFormTransport.requestHeader
                respond(raw, req.headers.get(header).contains(expected))
            }.orDie
        }
    )
end DatastarFormScenario
