package works.iterative.server.http.impl.blaze

import zio.*

case class BlazeServerConfig(host: String, port: Int)

object BlazeServerConfig:
    given config: Config[BlazeServerConfig] =
        import Config.*
        (
            string("host").withDefault("localhost") ++ int("port").withDefault(8080)
        ).nested("blaze").map(BlazeServerConfig.apply)
    end config
end BlazeServerConfig
