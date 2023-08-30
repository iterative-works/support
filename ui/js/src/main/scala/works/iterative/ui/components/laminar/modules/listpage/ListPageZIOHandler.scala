package works.iterative.ui.components.laminar.modules.listpage

import zio.*
import works.iterative.ui.ZIOEffectHandler
import zio.stream.ZStream

trait ListPageZIOHandler[T: Tag]:
  self: ListPageModel[T] =>

  type HandlerEnv = ListPageZIOHandler.Env[T]

  class Handler(onVisitDetail: T => UIO[Unit])
      extends ZIOEffectHandler[HandlerEnv, Effect, Action]:
    override def handle(e: Effect): ZStream[HandlerEnv, Throwable, Action] =
      e match
        case Effect.LoadItems =>
          fromZIO(
            ListPageHandler.loadItems[T]().map(Action.SetItems(_))
          )
        case Effect.ReportError(msg) =>
          fromZIOUnit(ListPageHandler.reportError[T](msg))
        case Effect.VisitDetail(item) =>
          fromZIOUnit(onVisitDetail(item))

object ListPageZIOHandler:
  type Env[T] = ListPageHandler[T]
