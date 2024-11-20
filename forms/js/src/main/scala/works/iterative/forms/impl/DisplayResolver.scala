package portaly
package forms
package impl

import zio.*
import com.raquo.laminar.api.L.*
import works.iterative.ui.model.forms.IdPath
import works.iterative.ui.model.forms.FormState
import works.iterative.core.MessageCatalogue
import works.iterative.core.Language
import works.iterative.core.UserMessage

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
            case "metpokyn_display" =>
                div(
                    child <-- ctx.state.get(id.up / "metpokyn").map:
                        case Some(v: String) if !v.isBlank() =>
                            div(
                                label(
                                    cls(
                                        "block text-sm font-medium leading-6 text-neutral-700"
                                    ),
                                    UserMessage("meridlo.metpokyn.label").asString
                                ),
                                div(
                                    cls("mt-1 text-sm text-neutral-900"),
                                    v
                                )
                            )
                        case _ => div()
                )
            case _ => div()
end CmiLiveDisplayResolver

class CmiReadOnlyDisplayResolver extends ReadOnlyHtmlDisplayResolver:
    override def resolve(id: IdPath, state: FormState)(using
        MessageCatalogue,
        Language
    ): HtmlElement =
        id.last match
            case "metpokyn_display" =>
                div(
                    state.getString(IdPath.full((id.up / "metpokyn").serialize)).map: v =>
                        if !v.isBlank() then
                            div(
                                label(
                                    cls(
                                        "block text-sm font-medium leading-6 text-neutral-700"
                                    ),
                                    UserMessage("meridlo.metpokyn.label").asString
                                ),
                                div(
                                    cls("mt-1 text-sm text-neutral-900"),
                                    v
                                )
                            )
                        else div()
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
