package works.iterative.tapir

import zio.*
import works.iterative.tapir.ApiError.AuthFailure
import works.iterative.core.auth.UserId

trait ApiKeyGuard:
    def checkApiKey(apiKey: String): IO[AuthFailure, UserId]

object ApiKeyGuard:
    val HeaderName = "X-API-Key"

    def checkApiKey(apiKey: String): ZIO[ApiKeyGuard, AuthFailure, UserId] =
        ZIO.serviceWithZIO(_.checkApiKey(apiKey))
end ApiKeyGuard
