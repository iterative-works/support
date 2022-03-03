package mdr.pdb.server
package api

import org.http4s.AuthedRoutes

class Routes():
  import CustomTapir.*

  val serverEndpoints: List[ZServerEndpoint[AppEnv, Any]] = Nil

  val routes: AuthedRoutes[AppAuth, AppTask] =
    CustomTapir.from(serverEndpoints).toRoutes.local(_.req)
