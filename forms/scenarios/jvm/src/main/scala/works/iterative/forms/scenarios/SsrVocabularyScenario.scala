// PURPOSE: SSR page for the conformance vocabulary form — every kind, a registry rule, all intents
// PURPOSE: The server-side half of the browser conformance pair; the SPA twin must behave alike

package works.iterative.forms.scenarios

import zio.http.{Response, Routes, Method, Root, Request, handler}
import zio.http.template.Html
import scalatags.Text.all.*
import works.iterative.forms.*
import works.iterative.core.{Language, MessageCatalogue}
import works.iterative.scenarios.Scenario
import works.iterative.ui.model.forms.{FormState, IdPath}

object SsrVocabularyScenario extends Scenario:
    val id = "ssrVocab"
    val label = "SSR Vocabulary"

    given MessageCatalogue = ConformanceCorpus.messages

    private val postAction = s"/$id/form"

    private val displayResolver: DisplayResolver[FormState, Frag] =
        new DisplayResolver[FormState, Frag]:
            def resolve(path: IdPath, state: FormState)(using MessageCatalogue, Language): Frag =
                frag()

    private val renderer = UIFormHtmlRenderer(displayResolver, FormTransport.htmx)
    private val builder = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))

    private def formTag(state: FormState, validation: FormValidationState): Tag =
        renderer.render(
            builder.buildForm(ConformanceCorpus.vocabularyForm, state, validation, None),
            postAction
        )

    override def page: Html =
        Html.raw(ScenarioHtml.shell(
            label,
            ScenarioHtml.htmxScript,
            formTag(FormData.parse(Map.empty), FormValidationState.valid)
        ))

    private def respondForm(
        state: FormData,
        hxRequest: Boolean,
        validation: FormValidationState = FormValidationState.valid
    ): Response =
        val tag = formTag(state, validation)
        if hxRequest then ScenarioHtml.htmlResponse(tag.render)
        else ScenarioHtml.htmlResponse(ScenarioHtml.shell(label, ScenarioHtml.htmxScript, tag))
    end respondForm

    private def submitted(data: FormData): Response =
        import zio.json.*
        val dump = data.data.map((path, values) =>
            path.toHtmlName -> values.map {
                case FieldValue.Text(value) => value
                case FieldValue.File(ref)   => ref.name
            }
        )
        ScenarioHtml.htmlResponse(ScenarioHtml.shell(
            label,
            ScenarioHtml.htmxScript,
            frag(h1("Vocabulary received"), p("Submitted data:"), pre(code(dump.toJson)))
        ))
    end submitted

    def respond(raw: Map[String, Seq[String]], hxRequest: Boolean): Response =
        val data = FormData.parse(raw)
        if raw.contains("__submit") then
            val validation = DeclaredValidation.validate(
                ConformanceCorpus.vocabularyForm,
                data,
                ConformanceCorpus.ruleRegistry
            )
            if validation.hasErrors then respondForm(data, hxRequest, validation)
            else submitted(data)
        else respondForm(data, hxRequest)
        end if
    end respond

    override val routes: Routes[Any, Nothing] = Routes(
        Method.GET / Root / id / "page" -> handler(ScenarioHtml.htmlResponse(ScenarioHtml.shell(
            label,
            ScenarioHtml.htmxScript,
            formTag(FormData.parse(Map.empty), FormValidationState.valid)
        ))),
        Method.POST / Root / id / "form" -> handler { (req: Request) =>
            req.body.asString.map { bodyString =>
                val raw = zio.http.QueryParams.decode(bodyString)
                    .map.view.mapValues(_.toSeq).toMap
                respond(raw, req.headers.get("HX-Request").contains("true"))
            }.orDie
        }
    )
end SsrVocabularyScenario
