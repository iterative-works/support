package works.iterative.tapir

import sttp.tapir.Endpoint
import works.iterative.core.auth.AccessToken
import sttp.capabilities.zio.ZioStreams

type ApiEndpoint[E, I, O] =
  Endpoint[AccessToken, I, ApiError[E], O, ZioStreams]
