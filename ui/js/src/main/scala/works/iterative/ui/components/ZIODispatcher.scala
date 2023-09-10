package works.iterative.ui.components

import zio.*

trait ZIODispatcher[+Env]:
  def dispatch(action: ZIO[Env, Nothing, Unit]): Unit

object ZIODispatcher:
  def fromRuntime[Env](using runtime: Runtime[Env]): ZIODispatcher[Env] =
    new ZIODispatcher[Env]:
      override def dispatch(action: ZIO[Env, Nothing, Unit]): Unit =
        Unsafe.unsafe(implicit unsafe =>
          // TODO: do I need to cancel this on evenstream stop?
          val _ = runtime.unsafe.runToFuture(action)
        )
