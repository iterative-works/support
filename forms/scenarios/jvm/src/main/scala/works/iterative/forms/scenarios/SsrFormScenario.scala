// PURPOSE: Scenario proving the server-side HTML form loop: GET renders, POST validates and re-renders
// PURPOSE: Exercises conditions, repeated add/remove and declared validation without any client-side app

package works.iterative.forms.scenarios

import zio.http.{Response, Routes, Method, Root, Request, handler}
import zio.http.template.Html
import scalatags.Text.all.*
import works.iterative.forms.*
import works.iterative.core.{Language, MessageCatalogue}
import works.iterative.scenarios.Scenario
import works.iterative.ui.model.forms.{FormState, IdPath}

object SsrFormScenario extends Scenario:
    val id = "ssrForm"
    val label = "SSR Form"

    val formDeclaration: Form = InquiryProofForm.declaration

    given MessageCatalogue = InquiryProofForm.messages

    private val itemsPath = IdPath.full("inquiry.items")
    private val addItemKey = "inquiry.controls.addItem"
    private val removeItemKey = "inquiry\\.items\\.([^.]+)\\.row\\.remove".r
    private val postAction = s"/$id/form"

    val initialState: FormData =
        FormData.parse(InquiryProofForm.initialItems)

    private val displayResolver: DisplayResolver[FormState, Frag] =
        new DisplayResolver[FormState, Frag]:
            def resolve(path: IdPath, state: FormState)(using MessageCatalogue, Language): Frag =
                val kind = state.getString(IdPath.full("inquiry.request.kind")).getOrElse("quote")
                val items = state.itemsFor(itemsPath).size
                p(s"You are requesting a $kind with $items item(s).")

    private val renderer = UIFormHtmlRenderer(displayResolver)
    private val builder = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))

    def renderFormTag(form: Form, state: FormState, validation: FormValidationState): Tag =
        renderer.render(builder.buildForm(form, state, validation, None), postAction)

    private def shell(inner: Frag): String = ScenarioHtml.shell("SSR Form", inner)

    private def htmlResponse(content: String): Response = ScenarioHtml.htmlResponse(content)

    override def page: Html =
        Html.raw(shell(renderFormTag(formDeclaration, initialState, FormValidationState.valid)))

    def respond(raw: Map[String, Seq[String]], hxRequest: Boolean): Response =
        val data = FormData.parse(raw)
        val removed = raw.keys.collectFirst { case removeItemKey(key) => key }
        if raw.contains(addItemKey) then
            val nextIndex = data.itemsFor(itemsPath)
                .flatMap((key, _) => key.stripPrefix("i").toIntOption)
                .maxOption.getOrElse(0) + 1
            respondForm(data.add(itemsPath / "__items", s"i$nextIndex:row"), hxRequest)
        else
            removed match
                case Some(key) =>
                    val remaining = data.itemsFor(itemsPath)
                        .filterNot(_._1 == key)
                        .map((k, t) => s"$k:$t")
                    val cleaned = data
                        .filterKeys(!_.serialize.startsWith(s"inquiry.items.$key."))
                        .set(itemsPath / "__items", remaining)
                    respondForm(cleaned, hxRequest)
                case None if raw.contains("__submit") =>
                    val validation = DeclaredValidation.validate(formDeclaration, data)
                    if validation.hasErrors then respondForm(data, hxRequest, validation)
                    else submitted(data)
                case None =>
                    respondForm(data, hxRequest)
        end if
    end respond

    private def respondForm(
        state: FormData,
        hxRequest: Boolean,
        validation: FormValidationState = FormValidationState.valid
    ): Response =
        val formTag = renderFormTag(formDeclaration, state, validation)
        if hxRequest then htmlResponse(formTag.render)
        else htmlResponse(shell(formTag))
    end respondForm

    private def submitted(data: FormData): Response =
        import zio.json.*
        val dump = data.data.map((path, values) =>
            path.toHtmlName -> values.map {
                case FieldValue.Text(value) => value
                case FieldValue.File(ref)   => ref.name
            }
        )
        htmlResponse(shell(frag(
            h1("Inquiry received"),
            p("Submitted data:"),
            pre(code(dump.toJson))
        )))
    end submitted

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
