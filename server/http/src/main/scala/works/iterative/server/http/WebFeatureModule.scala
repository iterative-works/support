package works.iterative.server.http

import org.http4s.HttpRoutes
import cats.syntax.all.*
import cats.Monad

trait WebFeatureModule[F[_]]:
    def routes: HttpRoutes[F]

object WebFeatureModule:
    def combineRoutes[F[_]: Monad](modules: WebFeatureModule[F]*): HttpRoutes[F] =
        modules.foldLeft(HttpRoutes.empty[F])(_ <+> _.routes)
