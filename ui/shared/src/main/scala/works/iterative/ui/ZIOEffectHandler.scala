package works.iterative.ui

import zio.stream.*

trait ZIOEffectHandler[Env, Effect, Action]:
  def handle(e: Effect): ZStream[Env, Throwable, Action]
