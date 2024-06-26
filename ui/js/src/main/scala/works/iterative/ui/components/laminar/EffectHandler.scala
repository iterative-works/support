package works.iterative.ui
package components.laminar

import zio.*
import com.raquo.laminar.api.L.*

trait EffectHandler[E, A]:
    def apply(
        effects: EventStream[E],
        actions: Observer[A]
    ): Modifier[HtmlElement]
end EffectHandler

object EffectHandler:
    given zioEffectHandler[Env, E, A](using
        Runtime[Env]
    ): Conversion[ZIOEffectHandler[Env, E, A], EffectHandler[E, A]] with
        def apply(h: ZIOEffectHandler[Env, E, A]): EffectHandler[E, A] =
            LaminarZIOEffectHandler(h)
    end zioEffectHandler

    def loggingHandler[E, A](name: String)(
        underlying: EffectHandler[E, A]
    ): EffectHandler[E, A] =
        new EffectHandler[E, A]:
            def apply(
                effects: EventStream[E],
                actions: Observer[A]
            ): Modifier[HtmlElement] =
                underlying(
                    effects.debugWithName(s"$name effects").debugLog(),
                    actions.debugWithName(s"$name actions").debugLog()
                )
end EffectHandler

class LaminarZIOEffectHandler[Env, E, A](handler: ZIOEffectHandler[Env, E, A])(
    using runtime: Runtime[Env]
) extends EffectHandler[E, A]:

    def apply(
        effects: EventStream[E],
        actions: Observer[A]
    ): Modifier[HtmlElement] =
        onMountCallback(ctx =>
            val _ = effects.foreach { effect =>
                val _ = Unsafe.unsafely {
                    runtime.unsafe
                        .runToFuture(
                            handler.handle(effect).either.runForeach {
                                case Right(a) => ZIO.succeed(actions.onNext(a))
                                case Left(e)  => ZIO.succeed(actions.onError(e))
                            }
                        )
                }
            }(ctx.owner)
        )
end LaminarZIOEffectHandler
