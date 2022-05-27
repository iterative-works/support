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

trait CustomTapirPlatformSpecific extends ZTapir with SttpClientInterpreter:
  self: CustomTapir =>

  type Backend = SttpBackend[Task, ZioStreams & WebSockets]

  val clientLayer: TaskLayer[Backend] =
    HttpClientZioBackend.layerUsingClient(
      HttpClient
        .newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()
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
      // TODO: set auth header from client side somehow
      .header(
        "Cookie",
        // "PLAY_SESSION=eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7InN0YXRlIjoiZDkxNGU4ZmRmZjI2ZmQ5ZDdmOGI2MzhiIiwiYXBwX3JlZGlyZWN0IjoiaHR0cHM6Ly90YzE2My5jbWkuY3ovbWRyL2FwcCIsImVycm9yX3JlZGlyZWN0IjoiaHR0cHM6Ly90YzE2My5jbWkuY3ovbWRyL2F1dGhfZXJyb3IifSwibmJmIjoxNjUzMzI2NDY5LCJpYXQiOjE2NTMzMjY0Njl9.kiLc-hdyzmX9m3i63K2qnIxlKtGQ_DeqoSWuGgY0pco; pac4jSessionId=03b38525-4c5c-42df-974e-a7db3e42586b; _dd_s=logs=1&id=ab704848-2128-4011-a53e-5305b434e172&created=1653690073090&expire=1653690975966"
        "pac4jSessionId=8c583af3-dc10-46d9-8c44-a53a2c0f3e18; _dd_s=logs=1&id=36d46f53-9cee-4526-9da1-d9534d310584&created=1653688998526&expire=1653691476217"
      )
      .send(backend)
      .map(_.body)
