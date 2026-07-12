package works.iterative.forms.scenarios

import com.raquo.laminar.api.L.*
import works.iterative.forms.BaseIWFormElement
import works.iterative.forms.impl.LiveHtmlInterpreter
import works.iterative.forms.LayoutResolver
import works.iterative.forms.impl.FieldTypeResolver
import works.iterative.forms.impl.ValidationResolver
import works.iterative.forms.impl.LiveHtmlDisplayResolver
import works.iterative.forms.impl.FormCtx
import works.iterative.ui.model.forms.IdPath
import works.iterative.core.Language
import works.iterative.core.MessageCatalogue
import works.iterative.forms.impl.ButtonHandler
import works.iterative.forms.impl.PersistenceProvider
import works.iterative.forms.impl.LiveFormHooks

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
