package works.iterative.server.http

import works.iterative.tapir.CustomTapir.*
import works.iterative.core.auth.CurrentUser

final case class HttpApplication[Env](
    secureEndpoints: List[ZServerEndpoint[Env & CurrentUser, Any]],
    publicEndpoints: List[ZServerEndpoint[Env, Any]]
)
