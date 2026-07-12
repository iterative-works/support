// PURPOSE: Custom-element base rendering a fetched form declaration through LiveHtmlInterpreter
// PURPOSE: Manages the Laminar root across the web-component lifecycle callbacks

package works.iterative.forms

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L
import org.scalajs.dom.*
import com.raquo.laminar.nodes.DetachedRoot
import works.iterative.forms.impl.LiveHtmlInterpreter
import works.iterative.forms.FormIdent

// scalafix:off DisableSyntax.var
// Web component lifecycle requires mutable state for Laminar root management
abstract class BaseIWFormElement extends HTMLElement:
    private var rootElem: Option[DetachedRoot[HtmlElement]] = None
// scalafix:on DisableSyntax.var

    def interpreter: LiveHtmlInterpreter

    def connectedCallback(): Unit =
        def liveForm(entityId: String, id: String, content: works.iterative.forms.Form) =
            interpreter.interpret(FormIdent(entityId, id), content, None)

        def attrOrDefault(attr: String, default: String) =
            Option(this.getAttribute(attr)).filterNot(_.isBlank()).getOrElse(default)

        // Render the element detached
        rootElem = Option(renderDetached(
            div(
                child.maybe <-- FetchStream.get(attrOrDefault("src", "/default-form")).map(result =>
                    import zio.json.*
                    import works.iterative.forms.service.impl.rest.FormPersistenceCodecs.given
                    result.fromJson[works.iterative.forms.Form].toOption.map(
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
