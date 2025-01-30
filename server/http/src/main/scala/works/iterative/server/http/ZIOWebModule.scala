package works.iterative.server.http

import zio.*
import zio.interop.catz.*
import org.http4s.*

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
