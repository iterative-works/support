package works.iterative.core.auth

import zio.*

/** A service that provides the current user, if any.
  */
trait CurrentUser:
  def currentUser: UIO[Option[UserId]]

object CurrentUser:
  def currentUser: URIO[CurrentUser, Option[UserId]] =
    ZIO.serviceWithZIO(_.currentUser)
