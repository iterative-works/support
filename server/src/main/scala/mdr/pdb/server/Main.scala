package mdr.pdb.server

import zio.*
import org.pac4j.core.profile.CommonProfile

type AppEnv = ZEnv
type AppTask = RIO[AppEnv, *]
type AppAuth = List[CommonProfile]

object Main extends ZIOAppDefault:

  lazy val runtimeLayer = ZLayer.fromZIO(ZIO.runtime[AppEnv])
  lazy val securityLayer =
    security.Pac4jSecurityConfig.fromEnv ++ runtimeLayer >>> security.Pac4jHttpSecurity.layer
  lazy val appLayer =
    AppConfig.fromEnv ++ securityLayer >>> HttpApplicationLive.layer
  lazy val serverLayer =
    blaze.BlazeServerConfig.fromEnv >+> appLayer >>> blaze.BlazeHttpServer.layer

  override def run =
    for {
      server <- ZIO
        .service[HttpServer]
        .provideCustom(serverLayer)
      _ <- server.serve()
    } yield ()
