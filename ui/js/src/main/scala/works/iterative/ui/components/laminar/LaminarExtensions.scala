package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.*
import works.iterative.core.UserMessage
import zio.IsSubtypeOfError
import works.iterative.ui.model.Computable
import works.iterative.core.auth.PermissionOp
import works.iterative.core.auth.PermissionTarget
import org.scalajs.dom
import com.raquo.airstream.split.Splittable

object LaminarExtensions
    extends I18NExtensions
    with ZIOInteropExtensions
    with ActionExtensions
    with AirstreamExtensions

trait AirstreamExtensions:
    given Splittable[Computable] with
        override def map[A, B](inputs: Computable[A], project: A => B): Computable[B] =
            import Computable.*
            inputs match
                case Uninitialized             => Uninitialized
                case Computing(start)          => Computing(start)
                case Ready(model)              => Ready(project(model))
                case Failed(error)             => Failed(error)
                case Recomputing(start, model) => Recomputing(start, project(model))
            end match
        end map

        override def foreach[A](inputs: Computable[A], f: A => Unit): Unit =
            import Computable.*
            inputs match
                case Ready(model)              => f(model)
                case Recomputing(start, model) => f(model)
                case _                         => ()
            end match
        end foreach

        override def empty[A]: Computable[A] = Computable.Uninitialized
    end given
end AirstreamExtensions

trait ActionExtensions:
    extension (action: works.iterative.core.Action)
        def mods: HtmlMod = modSeq(
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
                            yield (PermissionOp.unsafe(act), PermissionTarget.unsafe(arg))
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
                    val _ = Unsafe.unsafe { implicit unsafe =>
                        runtime.unsafe.fork(fiberRuntime.interrupt)
                    }
                    fiberRuntime = null
                else ()
        )
    end zioToEventStream

    def toZIOObserver[A, R](effect: A => ZIO[R, Nothing, Unit])(using
        runtime: Runtime[R]
    ): Observer[A] =
        Observer[A]: a =>
            val _ = Unsafe.unsafely {
                runtime.unsafe.fork(effect(a))
            }
            ()

    extension (o: Observer.type)
        def fromZIO[A, R](effect: A => ZIO[R, Nothing, Unit])(using
            runtime: Runtime[R]
        ): Observer[A] = toZIOObserver(effect)

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
                        val _ = Unsafe.unsafe { implicit unsafe =>
                            runtime.unsafe.fork(fiberRuntime.interrupt)
                        }
                        fiberRuntime = null
                    else ()
            )
        end toEventStreamWith
    end extension
end ZIOInteropExtensions
