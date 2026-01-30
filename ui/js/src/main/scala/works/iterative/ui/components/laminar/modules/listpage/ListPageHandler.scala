package works.iterative.ui.components.laminar.modules.listpage

import zio.*
import works.iterative.core.UserMessage

trait ListPageHandler[T, Q]:
    def loadItems(query: Q): UIO[List[T]]
    def reportError(message: UserMessage): UIO[Unit]

object ListPageHandler:
    def loadItems[T: Tag, Q: Tag](
        query: Q
    ): URIO[ListPageHandler[T, Q], List[T]] =
        ZIO.serviceWithZIO(_.loadItems(query))
    def reportError[T: Tag, Q: Tag](
        message: UserMessage
    ): URIO[ListPageHandler[T, Q], Unit] =
        ZIO.serviceWithZIO(_.reportError(message))
end ListPageHandler
