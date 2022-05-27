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
      .header(
        "Cookie",
        "PLAY_SESSION=eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7ImVycm9yX3JlZGlyZWN0IjoiaHR0cDovL2xvY2FsaG9zdDo5MDAwL21kci9hdXRoX2Vycm9yIiwiY3NyZlRva2VuIjoiNGUwNzk1M2Q4NjU3MmZkMGNhNTY5NTM0NTVhMDJiNmE4Y2RhZDc5OS0xNjI0ODk3NjQ1MTM0LWQzMmRlZmIyZDJhY2M0ZmM4YzI4ZTQyMCIsImFwcF9yZWRpcmVjdCI6Imh0dHA6Ly9sb2NhbGhvc3Q6OTAwMC9tZHIvYXBwIiwic3RhdGUiOiI5MWJiMjU5ZmMyZWMwNjUwODQxYWU2YTciLCJvc29ibmlDaXNsbyI6Ijk5OTEyMyIsInJvbGUiOiJhZG1pbmlzdHJhdG9yIn0sIm5iZiI6MTYyNDg5NzY0NSwiaWF0IjoxNjI0ODk3NjQ1fQ._P_wODFg1mbWxVkkLLCo74bCnsmcBFgKvbVECCnnrp0; _dd_s=logs=1&id=c63a44e3-02db-47c6-9234-ee546ce72096&created=1653641211408&expire=1653645413320; pac4jCsrfToken=efdaefd57128407197c3912d76700401; pac4jSessionId=3723ae2d-6d41-4ce1-8eb5-1e9c385e19ce"
      )
      .send(backend)
      .map(_.body)
