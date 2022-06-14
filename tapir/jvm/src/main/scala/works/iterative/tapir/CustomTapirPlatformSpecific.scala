package works.iterative.tapir

import zio.*
import sttp.tapir.ztapir.ZTapir

import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp.WebSocketToPipe
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.capabilities.WebSockets
import sttp.client3.SttpBackend
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.capabilities.zio.ZioStreams
import sttp.client3.SttpBackendOptions
import sttp.client3.httpclient.HttpClientBackend
import java.net.http.HttpClient
import java.net.CookieHandler
import java.net.CookieManager
import java.net.HttpCookie

trait CustomTapirPlatformSpecific extends ZTapir with SttpClientInterpreter:
  self: CustomTapir =>

  type Backend = SttpBackend[Task, ZioStreams & WebSockets]

  val clientLayer: RLayer[zio.System & BaseUri, Backend] =
    ZLayer {
      for
        baseUri <- ZIO.service[BaseUri]
        sessionId <- zio.System.env("SESSION")
        result <- Task.attempt[HttpClient.Builder => HttpClient.Builder] {

          val addCookie: Option[HttpClient.Builder => HttpClient.Builder] =
            for
              uri <- baseUri.toUri.map(_.toJavaUri)
              v <- sessionId
            yield
              val mgr = new CookieManager()
              mgr.getCookieStore.add(
                baseUri.toUri.map(_.toJavaUri).orNull,
                HttpCookie("pac4jSession", v)
              )
              _.cookieHandler(mgr)

          addCookie.getOrElse(identity)
        }
      yield result
    }.flatMap(ch =>
      HttpClientZioBackend.layerUsingClient(
        ch.get
          .apply(
            HttpClient
              .newBuilder()
              .followRedirects(HttpClient.Redirect.NEVER)
          )
          .build()
      )
    )

  def makeClient[I, E, O](
      endpoint: PublicEndpoint[I, E, O, Any]
  )(using
      baseUri: BaseUri,
      backend: Backend,
      wsToPipe: WebSocketToPipe[Any]
  ): I => Task[O] =
    val req = toRequestThrowErrors(endpoint, baseUri.toUri)
    req(_)
      .followRedirects(false)
      .send(backend)
      .map(_.body)
