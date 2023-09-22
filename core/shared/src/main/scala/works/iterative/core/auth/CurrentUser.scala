package works.iterative.core.auth

import zio.*
import scala.Conversion
import works.iterative.core.UserHandle

final case class CurrentUser(userProfile: BasicProfile) extends UserProfile:
  export userProfile.*

object CurrentUser:
  given Conversion[CurrentUser, BasicProfile] = _.userProfile
  given Conversion[CurrentUser, UserHandle] = _.handle

  def use[A](f: CurrentUser ?=> A): URIO[CurrentUser, A] =
    ZIO.serviceWith(f(using _))

  def useZIO[R, E, A](
      f: CurrentUser ?=> ZIO[R, E, A]
  ): ZIO[CurrentUser & R, E, A] =
    ZIO.serviceWithZIO[CurrentUser](f(using _))
