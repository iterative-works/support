package works.iterative.ui

import zio.*
import zio.stream.*

trait ZIOEffectHandler[Env, Effect, Action]:
  def handle(e: Effect): ZStream[Env, Throwable, Action]

  def fromZIO(
      zio: ZIO[Env, Throwable, Action]
  ): ZStream[Env, Throwable, Action] =
    ZStream.fromZIO(zio)

  def fromZIOOption(
      zio: ZIO[Env, Throwable, Option[Action]]
  ): ZStream[Env, Throwable, Action] =
    ZStream.fromZIO(zio).collect { case Some(a) => a }

  def fromZIOUnit(
      zio: ZIO[Env, Throwable, Unit]
  ): ZStream[Env, Throwable, Action] =
    ZStream.fromZIO(zio.as(Option.empty[Action])).collect { case Some(a) => a }
