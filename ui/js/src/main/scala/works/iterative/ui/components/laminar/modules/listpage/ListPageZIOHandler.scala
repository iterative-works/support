package works.iterative.ui.components.laminar.modules.listpage

import zio.*
import works.iterative.ui.ZIOEffectHandler
import zio.stream.ZStream

@scala.annotation.nowarn("msg=unused implicit parameter")
trait ListPageZIOHandler[T: Tag, Q: Tag]:
    self: ListPageModel[T, Q] =>

    class Handler(
        itemsHandler: ListPageHandler[T, Q],
        onVisitDetail: T => UIO[Unit]
    ) extends ZIOEffectHandler[Any, Effect, Action]:
        override def handle(e: Effect): ZStream[Any, Throwable, Action] =
            e match
                case Effect.LoadItems(q) =>
                    fromZIO(
                        itemsHandler.loadItems(q).map(Action.SetItems(_))
                    )
                case Effect.ReportError(msg) =>
                    fromZIOUnit(itemsHandler.reportError(msg))
                case Effect.VisitDetail(item) =>
                    fromZIOUnit(onVisitDetail(item))
    end Handler
end ListPageZIOHandler

object ListPageZIOHandler:
    type Env[T, Q] = ListPageHandler[T, Q]
