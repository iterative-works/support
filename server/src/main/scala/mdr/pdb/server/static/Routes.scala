package mdr.pdb.server
package static

import org.http4s.AuthedRoutes

class Routes(config: AppConfig):
  import CustomTapir.*

  val endpoints: List[ZServerEndpoint[AppEnv, Any]] =
    List(
      fileGetServerEndpoint("pdb" / "app")(
        s"${config.appPath}/index.html"
      ),
      filesGetServerEndpoint("pdb")(config.appPath)
    )

  val routes: AuthedRoutes[AppAuth, AppTask] =
    CustomTapir.from(endpoints).toRoutes.local(_.req)
