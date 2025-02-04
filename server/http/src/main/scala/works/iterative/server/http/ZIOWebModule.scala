package works.iterative.server.http

import zio.*
import zio.interop.catz.*
import org.http4s.*
import cats.syntax.all.*

trait ZIOWebModule[R] extends WebFeatureModule[RIO[R, *]]:
    type Env = R
    type WebTask[A] = RIO[R, A]

    /** Widens the environment type R to a supertype RR.
      *
      * This allows modules to be combined with modules that require additional dependencies.
      */
    def widen[RR <: R]: ZIOWebModule[RR] = this.asInstanceOf[ZIOWebModule[RR]]
end ZIOWebModule

object ZIOWebModule:
    def combineRoutes[RR](modules: ZIOWebModule[? >: RR]*): HttpRoutes[RIO[RR, *]] =
        val widened = modules.map(_.widen[RR])
        WebFeatureModule.combineRoutes(widened*)
end ZIOWebModule

/** Authenticated web module is similar to [[ZIOWebModule]], but it requires additional Context type
  * parameter. This type parameter is used by the authentication system to provide additional
  * information about the authenticated user.
  *
  * To convert [[AuthedZIOWebModule]] to [[ZIOWebModule]], you will need to provide authentication
  * middleware, e.g. by using pac4j:
  *
  * {{{
  * trait Pac4jZIOWebModule[R](pac4jSecurity: Pac4jHttpSecurity[R]) extends ZIOWebModule[R]:
  *     def wrap[C](inner: AuthedZIOWebModule[R, C])(
  *         toContext: List[CommonProfile] => Option[C],
  *         clients: Option[String] = Some("OidcClient")
  *     ): ZIOWebModule[R]
  * }}}
  */
trait AuthedZIOWebModule[R, C]:
    self =>
    type Env = R
    type Context = C
    type WebTask[A] = RIO[R, A]

    def publicRoutes: HttpRoutes[WebTask] = HttpRoutes.empty
    def authedRoutes: AuthedRoutes[C, WebTask]

    def widen[RR <: R]: AuthedZIOWebModule[RR, C] =
        this.asInstanceOf[AuthedZIOWebModule[RR, C]]

    def ++(other: AuthedZIOWebModule[R, C]): AuthedZIOWebModule[R, C] =
        new AuthedZIOWebModule[R, C]:
            override def publicRoutes = self.publicRoutes <+> other.publicRoutes
            override def authedRoutes = self.authedRoutes <+> other.authedRoutes
end AuthedZIOWebModule

object AuthedZIOWebModule:
    def empty[R, C]: AuthedZIOWebModule[R, C] = new AuthedZIOWebModule[R, C]:
        override def authedRoutes = AuthedRoutes.empty

    def combine[R, C](modules: AuthedZIOWebModule[R, C]*): AuthedZIOWebModule[R, C] =
        modules.foldLeft(empty[R, C])(_ ++ _)
end AuthedZIOWebModule
