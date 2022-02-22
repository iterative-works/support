package mdr.pdb.server

import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.{*, given}
import org.http4s.Request
import org.pac4j.http4s.Http4sWebContext

type AppTask = RIO[ZEnv, *]

object Main extends ZIOAppDefault:

  // TODO: move inside HttpApplication (using ZIO.runtime)
  private val contextBuilder =
    (req: Request[AppTask], conf: org.pac4j.core.config.Config) =>
      new Http4sWebContext[AppTask](
        req,
        conf.getSessionStore,
        runtime.unsafeRun(_)
      )

  override def run =
    for {
      server <- ZIO
        .service[HttpServer]
        .provideCustom(
          HttpApplicationLive.layer(contextBuilder) >>> BlazeHttpServer.layer
        )
      _ <- server.serve()
    } yield ()
