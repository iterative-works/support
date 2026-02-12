package works.iterative.server.http.tapir

import sttp.tapir.ztapir.*
import works.iterative.tapir.CustomTapir

/** A clean module that only focuses on defining Tapir endpoints and their implementations.
  *
  * This module is ZIO specific, and has R as environment. Also, it reports capabilities as C.
  */
trait TapirEndpointModule[R, C] extends CustomTapir:
    self =>

    /** Define the API endpoints without implementation logic */
    def endpoints: List[Endpoint[?, ?, ?, ?, ?]]

    /** Define the server endpoints with implementation logic */
    def serverEndpoints: List[ZServerEndpoint[R, C]]

    /** Widens the environment type R to a supertype RR */
    def widen[RR <: R, CC <: C]: TapirEndpointModule[RR, CC] =
        this.asInstanceOf[TapirEndpointModule[RR, CC]]

    /** Combines this module with another module */
    def ++(other: TapirEndpointModule[R, C]): TapirEndpointModule[R, C] =
        new TapirEndpointModule[R, C]:
            override def endpoints = self.endpoints ++ other.endpoints
            override def serverEndpoints = self.serverEndpoints ++ other.serverEndpoints
end TapirEndpointModule

object TapirEndpointModule:
    /** Creates an empty TapirEndpointModule */
    def empty[R, C]: TapirEndpointModule[R, C] = new TapirEndpointModule[R, C]:
        override def endpoints = List.empty
        override def serverEndpoints = List.empty

    /** Combines multiple TapirEndpointModules into a single module */
    def combine[R, C](modules: TapirEndpointModule[R, C]*): TapirEndpointModule[R, C] =
        modules.foldLeft(empty[R, C])(_ ++ _)
end TapirEndpointModule
