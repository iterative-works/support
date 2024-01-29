package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import works.iterative.core.UserMessage
import zio.IsSubtypeOfError
import works.iterative.ui.model.Computable
import works.iterative.core.auth.PermissionOp
import works.iterative.core.auth.PermissionTarget
import org.scalajs.dom

object LaminarExtensions
    extends I18NExtensions
    with ZIOInteropExtensions
    with ActionExtensions

trait ActionExtensions:
    extension (action: works.iterative.core.Action)
        def mods: HtmlMod = nodeSeq(
            dataAttr("action_op")(action.op.value),
            dataAttr("action_target")(action.target.toString())
        )

    object ActionLink:
        def unapply(
            evt: org.scalajs.dom.MouseEvent
        ): Option[(PermissionOp, PermissionTarget)] =
            evt.target match
            case t: dom.Element =>
                t.closest("[data-action_op]") match
                case el: dom.HTMLElement =>
                    for
                        act <- el.dataset.get("action_op")
                        arg <- el.dataset.get("action_target")
                    yield (PermissionOp(act), PermissionTarget.unsafe(arg))
                case _ => None
            case _ => None
    end ActionLink
end ActionExtensions

trait ZIOInteropExtensions:
    import zio.{Fiber, Runtime, Unsafe, ZIO}

    def zioToEventStream[R, E, A](effect: ZIO[R, E, A])(using runtime: Runtime[R])(using
        ev: E IsSubtypeOfError Throwable
    ): EventStream[A] =
        var fiberRuntime: Fiber.Runtime[E, A] = null
        EventStream.fromCustomSource(
            shouldStart = _ == 1,
            start = (fireValue, fireError, getStartIndex, getIsStarted) =>
                Unsafe.unsafe { implicit unsafe =>
                    fiberRuntime = runtime.unsafe.fork(effect)
                    fiberRuntime.unsafe.addObserver(exit =>
                        exit.foldExit(cause => fireError(cause.squash), fireValue)
                    )
                    fiberRuntime = null
                },
            stop = _ =>
                if fiberRuntime != null then
                    Unsafe.unsafe { implicit unsafe =>
                        runtime.unsafe.run(fiberRuntime.interrupt).ignore
                    }
                    fiberRuntime = null
                else ()
        )
    end zioToEventStream

    def syncZIOObserver[A, R](effect: A => ZIO[R, Nothing, Unit])(using
        runtime: Runtime[R]
    ): Observer[A] =
        Observer[A]: a =>
            Unsafe.unsafely {
                runtime.unsafe.run(effect(a))
            }
            ()

    extension (o: Observer.type)
        def fromZIO[A, R](effect: A => ZIO[R, Nothing, Unit])(using
            runtime: Runtime[R]
        ): Observer[A] = syncZIOObserver(effect)

    extension (e: EventStream.type)
        def fromZIO[R, E, A](effect: ZIO[R, E, A])(using
            runtime: Runtime[R]
        )(using ev: E IsSubtypeOfError Throwable): EventStream[A] =
            zioToEventStream(effect)

        def fromZIOOpt[R, E, A](effect: ZIO[R, E, Option[A]])(using
            runtime: Runtime[R]
        )(using ev: E IsSubtypeOfError Throwable): EventStream[A] =
            zioToEventStream(effect).collect { case Some(a) => a }
    end extension

    extension [R, E, O](effect: ZIO[R, E, O])
        def computableUpdate(using
            ev: E IsSubtypeOfError UserMessage
        ): ZIO[R, Nothing, Computable.Update[O]] =
            effect
                .map(Computable.Update.Done(_))
                .mapError(msg => Computable.Update.Failed(ev(msg)))
                .merge

        def toEventStream(using runtime: Runtime[R])(using
            ev: E IsSubtypeOfError Throwable
        ): EventStream[O] = zioToEventStream(effect)

        def toEventStreamWith(mapError: E => Throwable)(using
            runtime: Runtime[R]
        ): EventStream[O] =
            var fiberRuntime: Fiber.Runtime[E, O] = null
            EventStream.fromCustomSource(
                shouldStart = _ == 1,
                start = (fireValue, fireError, getStartIndex, getIsStarted) =>
                    Unsafe.unsafe { implicit unsafe =>
                        fiberRuntime = runtime.unsafe.fork(effect)
                        fiberRuntime.unsafe.addObserver(exit =>
                            exit.foldExit(
                                cause => fireError(cause.squashWith(mapError)),
                                fireValue
                            )
                        )
                        fiberRuntime = null
                    },
                stop = _ =>
                    if fiberRuntime != null then
                        Unsafe.unsafe { implicit unsafe =>
                            runtime.unsafe.run(fiberRuntime.interrupt).ignore
                        }
                        fiberRuntime = null
                    else ()
            )
        end toEventStreamWith
    end extension
end ZIOInteropExtensions
