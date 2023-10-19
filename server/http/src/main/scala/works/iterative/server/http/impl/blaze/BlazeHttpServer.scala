package works.iterative.server.http
package impl.blaze

import zio.*
import zio.interop.catz.*
import cats.syntax.semigroupk.*
import org.http4s.blaze.server.BlazeServerBuilder
import works.iterative.tapir.Http4sCustomTapir
import works.iterative.core.auth.CurrentUser
import works.iterative.server.http.impl.pac4j.Pac4jSecurityConfig
import org.http4s.*
import org.pac4j.http4s.Http4sWebContext
import works.iterative.server.http.impl.pac4j.Pac4jHttpSecurity
import cats.*
import cats.data.*
import cats.arrow.FunctionK
import works.iterative.tapir.BaseUri
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2
import com.nimbusds.jwt.JWTClaimsSet
import works.iterative.core.auth.Claim

class BlazeHttpServer(
    config: BlazeServerConfig,
    pac4jConfig: Pac4jSecurityConfig,
    baseUri: BaseUri,
    gatherClaims: JWTClaimsSet => Set[Claim] = _ => Set.empty
) extends HttpServer:
  override def serve[Env](app: HttpApplication[Env]): URIO[Env, Nothing] =
    type AppTask[A] = RIO[Env, A]
    type SecuredTask[A] = RIO[Env & CurrentUser, A]

    ZIO.runtime[Env].flatMap { runtime =>
      val contextBuilder =
        (req: Request[AppTask], conf: org.pac4j.core.config.Config) =>
          new Http4sWebContext[AppTask](
            req,
            conf.getSessionStore,
            t =>
              Unsafe.unsafe(implicit unsafe =>
                runtime.unsafe.run(t).getOrThrowFiberFailure()
              )
          )

      val pac4jSecurity =
        Pac4jHttpSecurity[AppTask](pac4jConfig, contextBuilder, gatherClaims)

      def provideCurrentUser(
          routes: HttpRoutes[SecuredTask]
      ): HttpRoutes[AppTask] =
        def secureRoutes: AuthedRoutes[CurrentUser, AppTask] = Kleisli { ctx =>
          val currentUser = ctx.context
          val userEnv = ZEnvironment(currentUser)

          // Just add CurrentUser to the env, the effect does not need it anyway
          val widenCurrentUser: AppTask ~> SecuredTask =
            new FunctionK[AppTask, SecuredTask]:
              override def apply[A](fa: AppTask[A]): SecuredTask[A] = fa

          // Provide
          val eliminateCurrentUser: SecuredTask ~> AppTask =
            new FunctionK[SecuredTask, AppTask]:
              override def apply[A](fa: SecuredTask[A]): AppTask[A] =
                fa.provideSomeEnvironment[Env](env => env ++ userEnv)

          routes
            .run(ctx.req.mapK(widenCurrentUser))
            .map(_.mapK(eliminateCurrentUser))
            .mapK(eliminateCurrentUser)
        }

        pac4jSecurity.secure(secureRoutes)

      val publicInterpreter = new Http4sCustomTapir[Env] {}
      val securedInterpreter = new Http4sCustomTapir[Env & CurrentUser] {}

      val publicRoutes: HttpRoutes[AppTask] =
        publicInterpreter.from(app.publicEndpoints).toRoutes
      val securedRoutes: HttpRoutes[SecuredTask] =
        securedInterpreter.from(app.secureEndpoints).toRoutes

      val wsRoutes: WebSocketBuilder2[AppTask] => HttpRoutes[AppTask] =
        publicInterpreter.fromWebSocket(app.wsEndpoints).toRoutes

      val eliminated: HttpRoutes[AppTask] = provideCurrentUser(securedRoutes)

      def withBaseUri(routes: HttpRoutes[AppTask]): HttpRoutes[AppTask] =
        baseUri.value match
          case Some(u) => Router(u.toString -> routes)
          case _       => routes

      BlazeServerBuilder[AppTask]
        .bindHttp(config.port, config.host)
        .withHttpWebSocketApp(wsb =>
          (pac4jSecurity.route <+> withBaseUri(
            wsRoutes(wsb) <+> publicRoutes <+> eliminated
          )).orNotFound
        )
        .serve
        .compile
        .drain
        .orDie *> ZIO.never
    }

object BlazeHttpServer:
  def layer(
      gatherClaims: JWTClaimsSet => Set[Claim] = _ => Set.empty
  ): RLayer[BlazeServerConfig & Pac4jSecurityConfig & BaseUri, HttpServer] =
    ZLayer {
      for
        config <- ZIO.service[BlazeServerConfig]
        pac4jConfig <- ZIO.service[Pac4jSecurityConfig]
        baseUri <- ZIO.service[BaseUri]
      yield BlazeHttpServer(config, pac4jConfig, baseUri, gatherClaims)
    }
