package works.iterative.ui.components.laminar.modules.formpage

import zio.*
import works.iterative.ui.ZIOEffectHandler
import zio.stream.ZStream

trait FormPageZIOHandler[T: Tag, K: Tag]:
    self: FormPageModel[T] =>

    type HandlerEnv = FormPageZIOHandler.Env[T, K]

    class Handler(key: K) extends ZIOEffectHandler[HandlerEnv, Effect, Action]:
        override def handle(e: Effect): ZStream[HandlerEnv, Throwable, Action] =
            e match
                case Effect.LoadInitialValue =>
                    fromZIO(
                        FormPageHandler.initialValue(key).map(Action.SetInitialValue(_))
                    )
                case Effect.Submit(value) =>
                    fromZIO(FormPageHandler.submit(key, value).as(Action.Submitted))
                case Effect.ReportError(msg) =>
                    fromZIOUnit(FormPageHandler.reportError(msg))
                case Effect.Cancel =>
                    fromZIOUnit(FormPageHandler.cancel(key))
    end Handler
end FormPageZIOHandler

object FormPageZIOHandler:
    type Env[T, K] = FormPageHandler[T, K]
