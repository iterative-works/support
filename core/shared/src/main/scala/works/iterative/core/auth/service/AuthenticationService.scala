package works.iterative.core.auth
package service

import zio.*
import works.iterative.core.UserMessage
import works.iterative.core.HasUserMessage

sealed abstract class AuthenticationError(val userMessage: UserMessage)
    extends RuntimeException(s"Authentication error: ${userMessage}")
    with HasUserMessage

object AuthenticationError:
  case object NotLoggedIn
      extends AuthenticationError(UserMessage("error.not.logged.in"))

trait AuthenticationService:
  def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit]
  def currentUserInfo: UIO[Option[AuthedUserInfo]]

  def currentAccessToken: UIO[Option[AccessToken]] =
    currentUserInfo.map(_.map(_.token))

  def provideCurrentUser[R, E, A](
      effect: ZIO[R & CurrentUser, E, A]
  ): ZIO[R, E | AuthenticationError, A] =
    currentUserInfo.flatMap {
      case Some(info) =>
        effect.provideSome[R](ZLayer.succeed(CurrentUser(info.profile)))
      case None => ZIO.fail(AuthenticationError.NotLoggedIn)
    }
