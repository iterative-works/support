package works.iterative.server.http

import zio.*
import org.http4s.HttpRoutes

trait ModuleRegistry[R]:
    self =>

    def modules: List[ZIOWebModule[R]]

    def routes: HttpRoutes[RIO[R, *]] =
        ZIOWebModule.combineRoutes(modules*)

    def ++(other: ModuleRegistry[R]): ModuleRegistry[R] = new ModuleRegistry[R]:
        override def modules = self.modules ++ other.modules

    def widen[R2 <: R]: ModuleRegistry[R2] = new ModuleRegistry[R2]:
        override def modules = self.modules.map(_.widen[R2])
end ModuleRegistry

object ModuleRegistry:
    def apply[R](module: ZIOWebModule[R]): ModuleRegistry[R] = new ModuleRegistry[R]:
        override def modules = List(module)

    def empty[R]: ModuleRegistry[R] = new ModuleRegistry[R]:
        override def modules = Nil
end ModuleRegistry
