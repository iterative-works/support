// PURPOSE: Registers the scenario custom elements rendering forms through LiveHtmlInterpreter
// PURPOSE: iw-form hosts the proof form, iw-vocab-form the conformance vocabulary form

package works.iterative.forms.scenarios

import com.raquo.laminar.api.L.*
import works.iterative.core.{Language, MessageCatalogue}
import works.iterative.forms.{BaseIWFormElement, LayoutResolver}
import works.iterative.forms.impl.{
    ButtonHandler,
    FieldTypeResolver,
    FormCtx,
    LiveForm,
    LiveFormHooks,
    LiveHtmlDisplayResolver,
    LiveHtmlInterpreter,
    PersistenceProvider,
    ValidationResolver
}
import works.iterative.ui.model.forms.IdPath

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport

@JSExportTopLevel("RegisterElement")
object Main:
    @JSExport("main")
    def main(): Unit =
        org.scalajs.dom.window.customElements.define(
            "iw-form",
            scala.scalajs.js.constructorOf[ScenarioIWFormElement]
        )
        org.scalajs.dom.window.customElements.define(
            "iw-vocab-form",
            scala.scalajs.js.constructorOf[VocabularyIWFormElement]
        )
    end main
end Main

class ScenarioIWFormElement extends BaseIWFormElement:
    private val messages = InquiryProofForm.messages
    private given MessageCatalogue = messages
    private given Language = Language.EN

    // The SPA twin of the SSR scenario's display resolver: kind and item count from live state
    private val summaryDisplay = new LiveHtmlDisplayResolver:
        override def resolve(id: IdPath, ctx: FormCtx)(using
            MessageCatalogue,
            Language
        ): HtmlElement =
            val kind = ctx.state.get(IdPath.full("inquiry.request.kind")).map(
                _.collect { case value: String if value.nonEmpty => value }.getOrElse("quote")
            )
            val items = ctx.state.under(IdPath.full("inquiry.items")).map(
                _.keys.flatMap(_.toHtmlName.split('.').drop(2).headOption).toSet.size
            )
            p(
                idAttr(id.toHtmlId),
                child.text <-- kind.combineWithFn(items): (k, n) =>
                    s"You are requesting a $k with $n item(s)."
            )
        end resolve

    override def interpreter = new LiveHtmlInterpreter(
        LayoutResolver.grid(PartialFunction.empty),
        FieldTypeResolver.empty,
        ValidationResolver.empty,
        summaryDisplay,
        ButtonHandler.empty,
        PersistenceProvider.empty,
        LiveFormHooks.empty,
        new SimpleFormComponents
    )

    override def formContent(form: LiveForm): HtmlElement =
        ScenarioSubmitChrome.wrap(
            form,
            "inquiry-submit",
            messages("inquiry.submit"),
            "/spaForm/submit",
            "Inquiry received"
        )
end ScenarioIWFormElement

class VocabularyIWFormElement extends BaseIWFormElement:
    private val messages = ConformanceCorpus.messages
    private given MessageCatalogue = messages
    private given Language = Language.EN

    private val noDisplays = new LiveHtmlDisplayResolver:
        override def resolve(id: IdPath, ctx: FormCtx)(using
            MessageCatalogue,
            Language
        ): HtmlElement = div(idAttr(id.toHtmlId))

    override def interpreter = new LiveHtmlInterpreter(
        LayoutResolver.grid(PartialFunction.empty),
        FieldTypeResolver.empty,
        ValidationResolver.empty,
        noDisplays,
        ButtonHandler.empty,
        PersistenceProvider.empty,
        LiveFormHooks.empty,
        new SimpleFormComponents,
        ruleRegistry = ConformanceCorpus.ruleRegistry
    )

    override def formContent(form: LiveForm): HtmlElement =
        ScenarioSubmitChrome.wrap(
            form,
            "vocab-submit",
            messages("vocab.submit"),
            "/spaVocab/submit",
            "Vocabulary received"
        )
end VocabularyIWFormElement
