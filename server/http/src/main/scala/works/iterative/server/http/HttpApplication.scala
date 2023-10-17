package works.iterative.server.http

import works.iterative.tapir.CustomTapir.*
import works.iterative.core.auth.CurrentUser
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

final case class HttpApplication[Env](
    secureEndpoints: List[
      ZServerEndpoint[Env & CurrentUser, ZioStreams]
    ],
    publicEndpoints: List[ZServerEndpoint[Env, ZioStreams]],
    wsEndpoints: List[
      ZServerEndpoint[Env, ZioStreams & WebSockets]
    ] = Nil
)
