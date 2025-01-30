package works.iterative.scalatags

import zio.*
import zio.interop.catz.*
import cats.syntax.all.*
import org.http4s.*
import cats.data.OptionT
import cats.arrow.FunctionK
import works.iterative.scalatags.components.ScalatagsErrorPageComponents

class ErrorHandlingMiddleware(helper: RoutesHelper, builder: ScalatagsErrorPageComponents)
    extends ScalatagsSupport:
    def apply[Env](routes: HttpRoutes[RIO[Env, *]]): HttpRoutes[RIO[Env, *]] =
        routes.mapF: zroute =>
            zroute
                .mapK:
                    FunctionK.lift:
                        [A] => (fa: RIO[Env, A]) => fa.resurrect
                .handleErrorWith: throwable =>
                    OptionT.liftF:
                        for
                            _ <- ZIO.logErrorCause(
                                "Unhandled error in routes",
                                Cause.fail(throwable)
                            )
                        yield Response(Status.InternalServerError)
                            .withEntity(
                                helper.fullPage(builder.errorPage(throwable))
                            )
end ErrorHandlingMiddleware

object ErrorHandlingMiddleware:
    val layer
        : ZLayer[RoutesHelper & ScalatagsErrorPageComponents, Nothing, ErrorHandlingMiddleware] =
        ZLayer.derive[ErrorHandlingMiddleware]

    def layer(components: ScalatagsErrorPageComponents)
        : ZLayer[RoutesHelper, Nothing, ErrorHandlingMiddleware] =
        ZLayer {
            for
                helper <- ZIO.service[RoutesHelper]
            yield ErrorHandlingMiddleware(helper, components)
        }
end ErrorHandlingMiddleware
