package works.iterative.ui
package components.laminar

import com.raquo.laminar.api.L.{*, given}

import zio.*
import com.raquo.airstream.core.Observer
import scala.annotation.implicitNotFound

trait EffectHandler[E, A]:
  def apply(
      effects: EventStream[E],
      actions: Observer[A]
  ): Modifier[HtmlElement]

object EffectHandler:
  given zioEffectHandler[Env, E, A](using
      Runtime[Env]
  ): Conversion[ZIOEffectHandler[Env, E, A], EffectHandler[E, A]] with
    def apply(h: ZIOEffectHandler[Env, E, A]): EffectHandler[E, A] =
      LaminarZIOEffectHandler(h)

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

class LaminarZIOEffectHandler[Env, E, A](handler: ZIOEffectHandler[Env, E, A])(
    using runtime: Runtime[Env]
) extends EffectHandler[E, A]:

  def apply(
      effects: EventStream[E],
      actions: Observer[A]
  ): Modifier[HtmlElement] =
    onMountCallback(ctx =>
      effects.foreach { effect =>
        Unsafe.unsafe { implicit unsafe =>
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
