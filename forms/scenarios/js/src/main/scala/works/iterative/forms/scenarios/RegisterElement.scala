// PURPOSE: Registers the iw-form custom element rendering the proof form through LiveHtmlInterpreter
// PURPOSE: Scenario submit chrome: invalid data shows errors, valid data posts and shows the received dump

package works.iterative.forms.scenarios

import com.raquo.laminar.api.L.*
import works.iterative.core.{Language, MessageCatalogue}
import works.iterative.forms.{BaseIWFormElement, LayoutResolver, ValidationState}
import works.iterative.forms.impl.{
    ButtonHandler,
    FieldTypeResolver,
    FormCtx,
    FormR,
    LiveForm,
    LiveFormHooks,
    LiveHtmlDisplayResolver,
    LiveHtmlInterpreter,
    PersistenceProvider,
    ValidationResolver
}
import works.iterative.ui.model.forms.IdPath
import zio.json.*

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
            p(child.text <-- kind.combineWithFn(items): (k, n) =>
                s"You are requesting a $k with $n item(s).")
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
        val submitted: Var[Option[String]] = Var(None)
        val clicks = new EventBus[Unit]
        val attempts = clicks.events.sample(form.data)
        div(
            attempts.collect { case state if !state.isValid => true } --> form.showErrors,
            attempts.collect { case ValidationState.Valid(data) => (data: FormR).toJson }
                .flatMapSwitch(json =>
                    FetchStream.post(
                        "/spaForm/submit",
                        _.body(json),
                        _.headers("Content-Type" -> "application/json")
                    )
                ).map(Some(_)) --> submitted.writer,
            child <-- submitted.signal.map {
                case Some(dumpJson) =>
                    div(
                        h1("Inquiry received"),
                        p("Submitted data:"),
                        pre(code(dumpJson))
                    )
                case None =>
                    div(
                        form.element,
                        button(
                            idAttr("inquiry-submit"),
                            tpe("button"),
                            messages("inquiry.submit"),
                            onClick.mapToUnit --> clicks.writer
                        )
                    )
            }
        )
    end formContent
end ScenarioIWFormElement
