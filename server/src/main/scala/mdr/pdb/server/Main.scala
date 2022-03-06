package mdr.pdb.server

import zio.*
import mdr.pdb.users.query.repo.*
import zio.config.ReadError
import zio.logging.*
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault:

  override def hook = SLF4J.slf4j(LogLevel.Debug)

  lazy val runtimeLayer: URLayer[AppEnv, Runtime[AppEnv]] =
    ZLayer.fromZIO(ZIO.runtime[AppEnv])

  lazy val securityLayer: ZLayer[AppEnv, ReadError[String], HttpSecurity] =
    security.Pac4jSecurityConfig.fromEnv ++ runtimeLayer >>> security.Pac4jHttpSecurity.layer

  lazy val httpAppLayer: ZLayer[AppEnv, ReadError[String], HttpApplication] =
    AppConfig.fromEnv ++ securityLayer >>> HttpApplicationLive.layer

  lazy val appEnvLayer: TaskLayer[UsersRepository] =
    MockUsersRepository.layer

  lazy val serverLayer: ZLayer[ZEnv, Throwable, HttpServer] =
    appEnvLayer >+> blaze.BlazeServerConfig.fromEnv >+> httpAppLayer >>> blaze.BlazeHttpServer.layer

  override def run =
    for {
      server <- ZIO
        .service[HttpServer]
        .provideCustom(serverLayer)
      _ <- server.serve().provideCustom(appEnvLayer)
    } yield ()
