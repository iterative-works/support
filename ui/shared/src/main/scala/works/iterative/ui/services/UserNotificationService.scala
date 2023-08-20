package works.iterative.ui.services

import works.iterative.core.UserMessage

import zio.*

/** A way for any module to notify the user about a success or failure
  */
trait UserNotificationService:
  def notify(level: UserNotificationService.Level, msg: UserMessage): UIO[Unit]
  def info(msg: UserMessage): UIO[Unit] =
    notify(UserNotificationService.Level.Info, msg)
  def warning(msg: UserMessage): UIO[Unit] =
    notify(UserNotificationService.Level.Warning, msg)
  def error(msg: UserMessage): UIO[Unit] =
    notify(UserNotificationService.Level.Error, msg)
  def debug(msg: UserMessage): UIO[Unit] =
    notify(UserNotificationService.Level.Debug, msg)
  def success(msg: UserMessage): UIO[Unit] =
    notify(UserNotificationService.Level.Success, msg)

object UserNotificationService:
  enum Level:
    case Info, Warning, Error, Debug, Success

  def notify(
      level: Level,
      msg: UserMessage
  ): URIO[UserNotificationService, Unit] =
    ZIO.serviceWithZIO(_.notify(level, msg))

  def info(msg: UserMessage): URIO[UserNotificationService, Unit] =
    ZIO.serviceWithZIO(_.info(msg))

  def warning(msg: UserMessage): URIO[UserNotificationService, Unit] =
    ZIO.serviceWithZIO(_.warning(msg))

  def error(msg: UserMessage): URIO[UserNotificationService, Unit] =
    ZIO.serviceWithZIO(_.error(msg))

  def debug(msg: UserMessage): URIO[UserNotificationService, Unit] =
    ZIO.serviceWithZIO(_.debug(msg))

  def success(msg: UserMessage): URIO[UserNotificationService, Unit] =
    ZIO.serviceWithZIO(_.success(msg))
