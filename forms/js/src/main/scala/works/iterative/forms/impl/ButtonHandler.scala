package portaly.forms
package impl

import zio.*
import com.raquo.laminar.api.L.*
import portaly.forms.impl.ButtonHandler.Result
import works.iterative.tapir.ClientEndpointFactory
import portaly.forms.service.impl.rest.AresEndpoints
import works.iterative.core.czech.ICO
import works.iterative.ui.laminar.*
import portaly.forms.service.impl.Ares.EkonomickySubjekt
import org.scalajs.dom.MouseEvent
import works.iterative.ui.model.forms.AbsolutePath

trait ButtonHandler:
    def register(btn: AbsolutePath)(using FormCtx): HtmlMod

object ButtonHandler:
    val empty: ButtonHandler = new ButtonHandler:
        def register(btn: AbsolutePath)(using FormCtx): HtmlMod = emptyMod
    enum Result:
        case Passed(btn: AbsolutePath)
        case Handled(btn: AbsolutePath, mutations: EventStream[FormR])
end ButtonHandler

class BaseButtonHandler(factory: ClientEndpointFactory, endpoints: AresEndpoints)(using
    Runtime[Any]
) extends ButtonHandler:
    private val aresClient = factory.make(endpoints.ares)

    override def register(btn: AbsolutePath)(using ctx: FormCtx): HtmlMod =
        val resultBus = EventBus[ButtonHandler.Result]()
        val handler = handle(btn)
        modSeq(
            onClick.compose(handler) --> resultBus.writer,
            resultBus.events.collect { case ButtonHandler.Result.Passed(btnId) =>
                btnId
            } --> ctx.buttonClicked,
            resultBus.events.collect {
                case ButtonHandler.Result.Handled(_, mutations) => mutations
            }.flattenSwitch --> ctx.updateValues
        )
    end register

    private def ico(btn: AbsolutePath)(using ctx: FormCtx) =
        val ic = ctx.state.validation(btn.up / "ico")
        ic.map[Option[String]] {
            case Some(ValidationState.Valid(v)) => v match
                    case i: String => Some(i)
                    case _         => None
            case _ => None
        }.setDisplayName("handle:ico")
    end ico

    def handle(btn: AbsolutePath)(using FormCtx): EventStream[MouseEvent] => EventStream[Result] =
        btn.last match
            case "complete_ares" => handleAres(btn)
            case _               => _ => EventStream.fromValue(Result.Passed(btn))

    def handleAres(btn: AbsolutePath)(using
        ctx: FormCtx
    ): EventStream[MouseEvent] => EventStream[Result] = es =>
        def responseToFormR(v: EkonomickySubjekt): FormR =
            FormR.Builder()
                .add("nazev", v.nazev)
                .add("ulice", v.ulice)
                .add("mesto", v.mesto)
                .add("psc", v.psc)
                .add("dic", v.dic)
                .add("stat", v.stat)
                .build(btn.up)
        end responseToFormR

        EventStream.fromValue(Result.Handled(
            btn,
            es.sample(ico(btn))
                .flatMapSwitch(ic =>
                    ZIO.foreach(ic.flatMap(i => ICO(i).toOption))(aresClient).map(
                        _.flatten.map(responseToFormR).getOrElse(FormR.empty)
                    ).toEventStream
                )
        ))
    end handleAres
end BaseButtonHandler
