package works.iterative.core.auth
package service

import zio.*

object Authentication extends AuthenticationService:
  private val currentUser: FiberRef[Option[AuthedUserInfo]] =
    Unsafe.unsafely(
      FiberRef.unsafe.make(None)
    )

  override val currentUserInfo: UIO[Option[AuthedUserInfo]] = currentUser.get

  override def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit] =
    currentUser.set(Some(AuthedUserInfo(token, profile)))
