package portaly
package forms
package impl

import zio.*
import com.raquo.laminar.api.L.*
import works.iterative.ui.model.forms.IdPath
import works.iterative.ui.model.forms.FormState
import works.iterative.core.MessageCatalogue
import works.iterative.core.Language
import works.iterative.ui.TimeUtils
import java.time.LocalDate
import works.iterative.core.UserMessage
import works.iterative.core.MessageArg

trait LiveHtmlDisplayResolver extends DisplayResolver[FormCtx, HtmlElement]:
    override def resolve(id: IdPath, ctx: FormCtx)(using MessageCatalogue, Language): HtmlElement

trait ReadOnlyHtmlDisplayResolver extends DisplayResolver[FormState, HtmlElement]:
    override def resolve(id: IdPath, state: FormState)(using
        MessageCatalogue,
        Language
    ): HtmlElement
end ReadOnlyHtmlDisplayResolver

class CmiLiveDisplayResolver extends LiveHtmlDisplayResolver:
    private def labeled(labelText: String, content: HtmlMod): HtmlElement =
        div(
            label(
                cls("block text-sm font-medium leading-6 text-gray-900 sm:pt-1.5"),
                labelText
            ),
            div(
                cls("mt-2 sm:col-span-2 sm:mt-0 text-sm text-gray-900 p-2"),
                content
            )
        )

    private def labeledString(baseId: IdPath, valueId: String)(using
        MessageCatalogue,
        FormCtx
    ): HtmlElement =
        val path = baseId / valueId
        labeled(
            UserMessage(path.toHtmlName).asString,
            child.text <-- FormCtx.ctx.state.get(path).map:
                case Some(e: String) => e
                case _               => ""
        )
    end labeledString

    override def resolve(id: IdPath, ctx: FormCtx)(using
        messages: MessageCatalogue,
        lang: Language
    ): HtmlElement =
        given FormCtx = ctx
        id.last match
            case "total_allowance_eur" =>
                div(
                    label(
                        cls("block text-sm font-medium leading-6 text-gray-900 sm:pt-1.5"),
                        forId(id.toHtmlId),
                        UserMessage("cbc.total").asString
                    ),
                    div(
                        cls("mt-2 sm:col-span-2 sm:mt-0 text-sm text-gray-900 p-2"),
                        "EUR ",
                        child.text <-- ctx.state.all(v =>
                            v.startsWith(id.up)
                        ).map(_.collect {
                            case e: String => scala.util.Try(e.replace(',', '.').toDouble).toOption
                        }.flatten.sum.toString)
                    )
                )
            case "total_mark" =>
                div(
                    label(
                        cls("block text-sm font-medium leading-6 text-gray-900 sm:pt-1.5"),
                        forId(id.toHtmlId),
                        UserMessage("cbc.total_mark").asString
                    ),
                    div(
                        cls("mt-2 sm:col-span-2 sm:mt-0 text-sm text-gray-900 p-2"),
                        child.text <-- ctx.state.all(v =>
                            v.startsWith(id.up)
                        ).map(_.collect {
                            case e: String => scala.util.Try(e.replace(',', '.').toDouble).toOption
                        }.flatten.sum.toString),
                        " / 10"
                    )
                )
            case "te_total_mark" =>
                div(
                    label(
                        cls("block text-sm font-medium leading-6 text-gray-900 sm:pt-1.5"),
                        forId(id.toHtmlId),
                        UserMessage("cbc.total_mark").asString
                    ),
                    div(
                        cls("mt-2 sm:col-span-2 sm:mt-0 text-sm text-gray-900 p-2"),
                        child.text <-- ctx.state.all(v =>
                            v.startsWith(id.up)
                        ).map(_.collect {
                            case e: String => scala.util.Try(e.replace(',', '.').toDouble).toOption
                        }.flatten.sum.toString),
                        " / 15"
                    )
                )
            case "evaluation_date" =>
                div(
                    label(
                        cls("block text-sm font-medium leading-6 text-gray-900 sm:pt-1.5"),
                        forId(id.toHtmlId),
                        UserMessage("cbc.evaluation_date").asString
                    ),
                    div(
                        cls("mt-2 sm:col-span-2 sm:mt-0 text-sm text-gray-900 p-2"),
                        TimeUtils.formatDate(LocalDate.now())
                    )
                )
            case "evaluator_name" =>
                labeledString(id.up, "evaluator_name")
            case "msa_preambule" =>
                div(
                    cls("flex flex-row items-center justify-between"),
                    labeledString(id.up, "organization_1"),
                    labeledString(id.up, "organization_2"),
                    labeledString(id.up, "application_number")
                )
            case "rms_preambule" =>
                div(
                    cls("flex flex-row items-center justify-between"),
                    labeledString(id.up, "organization"),
                    labeledString(id.up, "application_number")
                )
            case "call" =>
                labeledString(id.up, "call")
            case "privacy_policy" =>
                div(a(
                    cls("action-link text-sm pl-7 relative -top-6"),
                    href("https://www.cmi.cz/GDPRenglish?language=en"),
                    UserMessage("cbc.privacy_policy").asString
                ))
            case _ => div()
        end match
    end resolve
end CmiLiveDisplayResolver

class CmiReadOnlyDisplayResolver extends ReadOnlyHtmlDisplayResolver:
    override def resolve(id: IdPath, state: FormState)(using
        messages: MessageCatalogue,
        lang: Language
    ): HtmlElement =
        id.last match
            case "total_allowance_eur" =>
                div(
                    label(
                        cls("block text-sm font-medium leading-6 text-neutral-500"),
                        forId(id.toHtmlId),
                        UserMessage("cbc.total").asString
                    ),
                    div(
                        cls("sm:mt-0"),
                        span(
                            cls("text-sm"),
                            "EUR ",
                            state.all(v => v.startsWith(id.up)).collect {
                                case e: String =>
                                    scala.util.Try(e.replace(',', '.').toDouble).toOption
                            }.flatten.sum.toString
                        )
                    )
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
