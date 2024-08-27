package works.iterative.forms.scenarios

import com.raquo.laminar.api.L.*
import portaly.forms.impl.LiveHtmlInterpreter
import portaly.forms.LayoutResolver
import portaly.forms.impl.FieldTypeResolver
import portaly.forms.impl.ValidationResolver
import portaly.forms.impl.LiveHtmlDisplayResolver
import portaly.forms.impl.FormCtx
import works.iterative.ui.model.forms.IdPath
import works.iterative.core.Language
import works.iterative.core.MessageCatalogue
import portaly.forms.impl.ButtonHandler
import portaly.forms.impl.PersistenceProvider
import portaly.forms.impl.LiveFormHooks

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport

@JSExportTopLevel("Main")
object Main:
    @JSExport("main")
    def main(): Unit =
        org.scalajs.dom.window.customElements.define(
            "iw-form",
            scala.scalajs.js.constructorOf[ScenarioIWFormElement]
        )
end Main

class ScenarioIWFormElement extends BaseIWFormElement:
    override def interpreter = new LiveHtmlInterpreter(
        LayoutResolver.grid(PartialFunction.empty),
        FieldTypeResolver.empty,
        ValidationResolver.empty,
        new LiveHtmlDisplayResolver:
            override def resolve(id: IdPath, ctx: FormCtx)(using
                MessageCatalogue,
                Language
            ): HtmlElement = div()
        ,
        ButtonHandler.empty,
        PersistenceProvider.empty,
        LiveFormHooks.empty,
        new SimpleFormComponents
    )(using MessageCatalogue.debug, Language.CS)
end ScenarioIWFormElement
