package works.iterative.ui.components.laminar.modules.listpage

import zio.*
import works.iterative.core.UserMessage

trait ListPageHandler[T]:
  def loadItems(): UIO[List[T]]
  def reportError(message: UserMessage): UIO[Unit]

object ListPageHandler:
  def loadItems[T: Tag](): URIO[ListPageHandler[T], List[T]] =
    ZIO.serviceWithZIO(_.loadItems())
  def reportError[T: Tag](
      message: UserMessage
  ): URIO[ListPageHandler[T], Unit] =
    ZIO.serviceWithZIO(_.reportError(message))
