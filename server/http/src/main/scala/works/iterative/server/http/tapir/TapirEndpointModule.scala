package works.iterative.server.http.tapir

import sttp.tapir.*
import sttp.tapir.ztapir.*
import works.iterative.tapir.CustomTapir

/** A clean module that only focuses on defining Tapir endpoints and their implementations */
trait TapirEndpointModule[R] extends CustomTapir:
    self =>

    /** Define the API endpoints without implementation logic */
    def endpoints: List[Endpoint[?, ?, ?, ?, ?]]

    /** Define the server endpoints with implementation logic */
    def serverEndpoints: List[ZServerEndpoint[R, Any]]

    /** Widens the environment type R to a supertype RR */
    def widen[RR <: R]: TapirEndpointModule[RR] =
        this.asInstanceOf[TapirEndpointModule[RR]]

    /** Combines this module with another module */
    def ++(other: TapirEndpointModule[R]): TapirEndpointModule[R] =
        new TapirEndpointModule[R]:
            override def endpoints = self.endpoints ++ other.endpoints
            override def serverEndpoints = self.serverEndpoints ++ other.serverEndpoints
end TapirEndpointModule

object TapirEndpointModule:
    /** Creates an empty TapirEndpointModule */
    def empty[R]: TapirEndpointModule[R] = new TapirEndpointModule[R]:
        override def endpoints = List.empty
        override def serverEndpoints = List.empty

    /** Combines multiple TapirEndpointModules into a single module */
    def combine[R](modules: TapirEndpointModule[R]*): TapirEndpointModule[R] =
        modules.foldLeft(empty[R])(_ ++ _)
end TapirEndpointModule
