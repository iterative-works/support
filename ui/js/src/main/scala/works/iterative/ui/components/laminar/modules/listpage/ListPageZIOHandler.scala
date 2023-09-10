package works.iterative.ui.components.laminar.modules.listpage

import zio.*
import works.iterative.ui.ZIOEffectHandler
import zio.stream.ZStream

trait ListPageZIOHandler[T: Tag]:
  self: ListPageModel[T] =>

  class Handler(itemsHandler: ListPageHandler[T], onVisitDetail: T => UIO[Unit])
      extends ZIOEffectHandler[Any, Effect, Action]:
    override def handle(e: Effect): ZStream[Any, Throwable, Action] =
      e match
        case Effect.LoadItems =>
          fromZIO(
            itemsHandler.loadItems().map(Action.SetItems(_))
          )
        case Effect.ReportError(msg) =>
          fromZIOUnit(itemsHandler.reportError(msg))
        case Effect.VisitDetail(item) =>
          fromZIOUnit(onVisitDetail(item))

object ListPageZIOHandler:
  type Env[T] = ListPageHandler[T]
