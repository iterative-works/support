package portaly
package forms
package impl

import com.raquo.laminar.api.L.*
import works.iterative.ui.model.forms.IdPath
import works.iterative.ui.model.forms.FormState
import works.iterative.core.MessageCatalogue
import works.iterative.core.Language

trait LiveHtmlDisplayResolver extends DisplayResolver[FormCtx, HtmlElement]:
    override def resolve(id: IdPath, ctx: FormCtx)(using MessageCatalogue, Language): HtmlElement

trait ReadOnlyHtmlDisplayResolver extends DisplayResolver[FormState, HtmlElement]:
    override def resolve(id: IdPath, state: FormState)(using
        MessageCatalogue,
        Language
    ): HtmlElement
end ReadOnlyHtmlDisplayResolver
