package works.iterative.server.http.impl.blaze

import zio.*
import org.http4s.server.defaults

case class BlazeServerConfig(
    host: String,
    port: Int,
    responseHeaderTimeout: Duration = Duration.fromScala(defaults.ResponseTimeout),
    idleTimeout: Duration = Duration.fromScala(defaults.IdleTimeout)
)

object BlazeServerConfig:
    given config: Config[BlazeServerConfig] =
        import Config.*
        (
            string("host").withDefault("localhost") zip int("port").withDefault(8080) zip duration(
                "responseHeaderTimeout"
            ).withDefault(zio.Duration.fromScala(defaults.ResponseTimeout)) zip duration(
                "idleTimeout"
            ).withDefault(
                zio.Duration.fromScala(defaults.IdleTimeout)
            )
        ).nested("blaze").map(BlazeServerConfig.apply)
    end config
end BlazeServerConfig
