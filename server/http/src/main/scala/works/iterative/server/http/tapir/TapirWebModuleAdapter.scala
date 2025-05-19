package works.iterative.server.http.tapir

import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.server.http4s.Http4sServerOptions
import zio.RIO
import org.http4s.HttpRoutes
import works.iterative.server.http.WebFeatureModule
import zio.interop.catz.*
import sttp.capabilities.zio.ZioStreams

/** Adapts a TapirEndpointModule to a WebFeatureModule with HTTP routes */
object TapirWebModuleAdapter:
    /** Converts a TapirEndpointModule to a WebFeatureModule */
    def adapt[R, C >: ZioStreams](
        options: Http4sServerOptions[RIO[R, *]] = Http4sServerOptions.default,
        module: TapirEndpointModule[R, C]
    ): WebFeatureModule[RIO[R, *]] =
        new WebFeatureModule[RIO[R, *]]:
            override def routes: HttpRoutes[RIO[R, *]] =
                ZHttp4sServerInterpreter(options).from(module.serverEndpoints).toRoutes

    /** Combines multiple TapirEndpointModules and converts them to a single WebFeatureModule */
    def combine[R, C >: ZioStreams](
        options: Http4sServerOptions[RIO[R, *]] = Http4sServerOptions.default,
        modules: TapirEndpointModule[R, C]*
    ): WebFeatureModule[RIO[R, *]] =
        adapt(options, TapirEndpointModule.combine(modules*))
end TapirWebModuleAdapter
