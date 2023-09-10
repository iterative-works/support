package works.iterative.ui.components

import zio.*
import com.raquo.airstream.core.EventStream
import works.iterative.ui.components.laminar.LaminarExtensions.*

trait ZIODispatcher[+Env]:
  def dispatch(action: ZIO[Env, Nothing, Unit]): Unit
  def dispatchStream[A](action: ZIO[Env, Nothing, A]): EventStream[A]

object ZIODispatcher:
  def fromRuntime[Env](using runtime: Runtime[Env]): ZIODispatcher[Env] =
    new ZIODispatcher[Env]:
      override def dispatch(action: ZIO[Env, Nothing, Unit]): Unit =
        Unsafe.unsafe(implicit unsafe =>
          // TODO: do I need to cancel this on evenstream stop?
          val _ = runtime.unsafe.runToFuture(action)
        )

      override def dispatchStream[A](
          action: ZIO[Env, Nothing, A]
      ): EventStream[A] =
        action.toEventStream
