package works.iterative.forms.scenarios

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L
import org.scalajs.dom.*
import com.raquo.laminar.nodes.DetachedRoot
import portaly.forms.impl.LiveHtmlInterpreter
import portaly.forms.FormIdent

// scalafix:off DisableSyntax.var
// Web component lifecycle requires mutable state for Laminar root management
abstract class BaseIWFormElement extends HTMLElement:
    private var rootElem: Option[DetachedRoot[HtmlElement]] = None
// scalafix:on DisableSyntax.var

    def interpreter: LiveHtmlInterpreter

    def connectedCallback(): Unit =
        def liveForm(entityId: String, id: String, content: portaly.forms.Form) =
            interpreter.interpret(FormIdent(entityId, id), content, None)

        def attrOrDefault(attr: String, default: String) =
            Option(this.getAttribute(attr)).filterNot(_.isBlank()).getOrElse(default)

        // Render the element detached
        rootElem = Option(renderDetached(
            div(
                child.maybe <-- FetchStream.get(attrOrDefault("src", "/default-form")).map(result =>
                    import zio.json.*
                    import portaly.forms.service.impl.rest.FormPersistenceCodecs.given
                    result.fromJson[portaly.forms.Form].toOption.map(
                        liveForm(attrOrDefault("entity", "_"), attrOrDefault("form-id", "form"), _)
                    ).map(_.element)
                )
            ),
            activateNow = true
        ))

        rootElem.foreach(r => this.append(r.ref))
    end connectedCallback

    def disconnectedCallback(): Unit =
        rootElem.foreach(_.deactivate())

    def attributeChangedCallback(name: String, oldValue: String, newValue: String): Unit =
        ()

    def adoptedCallback(): Unit =
        ()
end BaseIWFormElement
