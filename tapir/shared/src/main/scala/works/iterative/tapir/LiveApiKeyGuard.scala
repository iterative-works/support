package works.iterative.tapir

import works.iterative.tapir.ApiError.AuthFailure

import zio.*
import works.iterative.core.auth.AuthenticationError
import works.iterative.core.auth.UserId

final case class ApiKeyConfig(
    apiKeys: Map[String, String]
)

object ApiKeyConfig:
    val config: Config[ApiKeyConfig] =
        import Config.*
        table(string("key")).nested("keys").nested("api").map(ApiKeyConfig.apply)
end ApiKeyConfig

class LiveApiKeyGuard(config: ApiKeyConfig) extends ApiKeyGuard:
    val apiToUser = config.apiKeys.toSeq.map: (id, key) =>
        key -> id
    .toMap

    override def checkApiKey(apiKey: String): IO[AuthFailure, UserId] =
        apiToUser.get(apiKey) match
            case Some(user) => ZIO.succeed(UserId.unsafe(user))
            case _ =>
                ZIO.fail(AuthFailure(AuthenticationError.InvalidCredentials))
end LiveApiKeyGuard

object LiveApiKeyGuard:
    val layer: Layer[Config.Error, ApiKeyGuard] = ZLayer {
        for
            config <- ZIO.config(ApiKeyConfig.config)
            _ <- ZIO.log(s"API config: ${config}")
        yield LiveApiKeyGuard(config)
    }
end LiveApiKeyGuard
