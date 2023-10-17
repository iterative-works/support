package works.iterative.tapir

import zio.*
import sttp.tapir.ztapir.ZTapir

import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp.WebSocketToPipe
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.client3.SttpBackend
import sttp.client3.httpclient.zio.HttpClientZioBackend
import java.net.http.HttpClient
import java.net.CookieHandler
import java.net.URI
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

trait CustomTapirPlatformSpecific extends ZTapir with SttpClientInterpreter:
  self: CustomTapir =>

  type Backend = SttpBackend[Task, ZioStreams & WebSockets]

  private def addSession(
      session: String
  ): HttpClient.Builder => HttpClient.Builder =
    _.cookieHandler(new CookieHandler {
      override def get(
          x: URI,
          y: java.util.Map[String, java.util.List[String]]
      ): java.util.Map[String, java.util.List[String]] =
        import scala.jdk.CollectionConverters.*
        Map(
          "Cookie" -> List(s"pac4jSessionId=$session").asJava
        ).asJava
      override def put(
          x: URI,
          y: java.util.Map[String, java.util.List[String]]
      ): Unit = ()
    })

  private def optionallyAddSession(
      session: Option[String]
  ): HttpClient.Builder => HttpClient.Builder =
    session match {
      case Some(s) => addSession(s)
      case None    => identity
    }

  def clientSessionLayer(sessionId: Option[String]): TaskLayer[Backend] =
    HttpClientZioBackend.layerUsingClient(
      optionallyAddSession(sessionId)(
        HttpClient
          .newBuilder()
          .followRedirects(HttpClient.Redirect.NEVER)
      )
        .build()
    )

  val clientLayer: TaskLayer[Backend] =
    ZLayer(zio.System.env("SESSION")).flatMap(sessionId =>
      clientSessionLayer(sessionId.get)
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
