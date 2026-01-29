package works.iterative.ui.components.laminar.modules.formpage

import zio.*
import works.iterative.core.UserMessage

trait FormPageHandler[T, K]:
    def initialValue(key: K): UIO[Option[T]]
    def submit(key: K, value: T): UIO[Unit]
    def cancel(key: K): UIO[Unit]
    def reportError(message: UserMessage): UIO[Unit]
end FormPageHandler

object FormPageHandler:
    def initialValue[K: Tag, T: Tag](
        key: K
    ): URIO[FormPageHandler[T, K], Option[T]] =
        ZIO.serviceWithZIO[FormPageHandler[T, K]](_.initialValue(key))
    def submit[K: Tag, T: Tag](
        key: K,
        value: T
    ): URIO[FormPageHandler[T, K], Unit] =
        ZIO.serviceWithZIO[FormPageHandler[T, K]](_.submit(key, value))
    def cancel[K: Tag, T: Tag](key: K): URIO[FormPageHandler[T, K], Unit] =
        ZIO.serviceWithZIO[FormPageHandler[T, K]](_.cancel(key))
    def reportError[K: Tag, T: Tag](
        message: UserMessage
    ): URIO[FormPageHandler[T, K], Unit] =
        ZIO.serviceWithZIO[FormPageHandler[T, K]](_.reportError(message))
end FormPageHandler
