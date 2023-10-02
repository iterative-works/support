package works.iterative.ui.services

import zio.*
import works.iterative.core.UserMessage
import works.iterative.core.MessageCatalogue

class ConsoleNotificationService(messages: MessageCatalogue)
    extends UserNotificationService:
  override def notify(
      level: UserNotificationService.Level,
      msg: UserMessage
  ): UIO[Unit] =
    ZIO.succeed(org.scalajs.dom.console.log(s"[$level] ${messages(msg)}"))

object ConsoleNotificationService:
  val layer: URLayer[MessageCatalogue, UserNotificationService] =
    ZLayer.derive[ConsoleNotificationService]
