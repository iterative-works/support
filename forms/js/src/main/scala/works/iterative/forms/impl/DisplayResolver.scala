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

    override def resolve(id: IdPath, ctx: FormCtx)(using MessageCatalogue, Language): HtmlElement =
        id.last match
            case "total_allowance_eur" =>
                div(
                    label(
                        cls("block text-sm font-medium leading-6 text-gray-900 sm:pt-1.5"),
                        forId(id.toHtmlId),
                        if lang == Language.EN then "Total" else "Celkem"
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
                        if lang == Language.EN then "Total mark" else "Celkem"
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
            case "evaluation_date" =>
                div(
                    label(
                        cls("block text-sm font-medium leading-6 text-gray-900 sm:pt-1.5"),
                        forId(id.toHtmlId),
                        if lang == Language.EN then "Date of evaluation" else "Datum hodnocení"
                    ),
                    div(
                        cls("mt-2 sm:col-span-2 sm:mt-0 text-sm text-gray-900 p-2"),
                        TimeUtils.formatDate(LocalDate.now())
                    )
                )
            case "evaluator_name" =>
                labeled(
                    if lang == Language.EN then "Name of evaluator" else "Jméno hodnotitele",
                    "Karel Novák"
                )
            case "preambule" =>
                div(
                    cls("flex flex-row items-center justify-between"),
                    labeled("Organization 1", "ACME, s.r.o."),
                    labeled("Organization 2", "FooBar, s.r.o."),
                    labeled("MSA application number", "MSA-24-0001")
                )
            case _ => div()
end CmiLiveDisplayResolver

class CmiReadOnlyDisplayResolver extends ReadOnlyHtmlDisplayResolver:
    override def resolve(id: IdPath, state: FormState)(using
        MessageCatalogue,
        Language
    ): HtmlElement =
        id.last match
            case "total_allowance_eur" =>
                div(
                    label(
                        cls("block text-sm font-medium leading-6 text-neutral-500"),
                        forId(id.toHtmlId),
                        if lang == Language.EN then "Total" else "Celkem"
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
