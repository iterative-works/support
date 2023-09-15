package works.iterative.server.http

import works.iterative.tapir.CustomTapir.*

final case class HttpApplication[Env](
    endpoints: List[ZServerEndpoint[Env, Any]]
)
