package portaly
package forms
package impl

import zio.*
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

class CmiLiveDisplayResolver extends LiveHtmlDisplayResolver:
    override def resolve(id: IdPath, ctx: FormCtx)(using MessageCatalogue, Language): HtmlElement =
        id.last match
            case "platba" =>
                div(
                    cls("text-blue-800 text-sm"),
                    "Pro přímé objednání a zaplacení služby je nutné zvolit ceníkové položky ze seznamu níže."
                )
            case _ => div()
end CmiLiveDisplayResolver

class CmiReadOnlyDisplayResolver extends ReadOnlyHtmlDisplayResolver:
    override def resolve(id: IdPath, state: FormState)(using
        MessageCatalogue,
        Language
    ): HtmlElement =
        id.last match
            case "platba" =>
                div(
                    cls("text-blue-800 text-sm"),
                    "Pro přímé objednání a zaplacení služby je nutné zvolit ceníkové položky ze seznamu níže."
                )
            case _ => div()
end CmiReadOnlyDisplayResolver

object DefaultDisplayResolvers:
    val layer: ULayer[LiveHtmlDisplayResolver & ReadOnlyHtmlDisplayResolver] =
        ZLayer.succeedEnvironment(
            ZEnvironment[LiveHtmlDisplayResolver, ReadOnlyHtmlDisplayResolver](
                CmiLiveDisplayResolver(),
                CmiReadOnlyDisplayResolver()
            )
        )
end DefaultDisplayResolvers
