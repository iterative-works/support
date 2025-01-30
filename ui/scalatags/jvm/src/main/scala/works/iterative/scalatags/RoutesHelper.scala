package works.iterative.scalatags

import zio.*
import scalatags.Text.all.*
import org.http4s.Headers
import org.typelevel.ci.*
import components.ScalatagsAppShell

// TODO: extract to shared code together with AppShell and ErrorHandlingMiddleware
// As the AppShell has type parameter, we would better make Scalatags-specific versions and extract these
class RoutesHelper(layout: ScalatagsAppShell):
    def fullPage(content: Frag): Frag = layout.wrap(content)

    def partial(content: Frag): Frag = content
end RoutesHelper

object RoutesHelper:
    val layer: ZLayer[ScalatagsAppShell, Nothing, RoutesHelper] =
        ZLayer.derive[RoutesHelper]

    def apply[R, E, A](f: RoutesHelper => ZIO[R, E, A])
        : ZIO[R & RoutesHelper, E, A] =
        ZIO.serviceWithZIO[RoutesHelper](f)

    /** Use fullPage or partial based on headers */
    def hx[R, E, A](headers: Headers)(f: (Frag => Frag) => ZIO[R, E, A])
        : ZIO[R & RoutesHelper, E, A] =
        ZIO.serviceWithZIO[RoutesHelper]: helper =>
            headers.get(ci"HX-Request") match
                case Some(_) => f(helper.partial)
                case None    => f(helper.fullPage)
end RoutesHelper
