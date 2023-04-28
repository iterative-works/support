package works.iterative.ui.services

import zio.*
import works.iterative.core.UserMessage

class ConsoleNotificationService extends UserNotificationService:
  override def notify(
      level: UserNotificationService.Level,
      msg: UserMessage
  ): UIO[Unit] =
    ZIO.succeed(org.scalajs.dom.console.log(s"[$level] $msg"))

object ConsoleNotificationService:
  val layer: ULayer[UserNotificationService] =
    ZLayer.succeed(ConsoleNotificationService())
