package works.iterative.akka

import akka.util.Timeout
import zio.*
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

case class AkkaConfig(timeout: Timeout =
    Timeout(scala.concurrent.duration.Duration(10L, TimeUnit.SECONDS)))

object AkkaConfig:
    given config: Config[AkkaConfig] =
        import Config.*
        (
            duration("timeout")
                .withDefault(10.seconds)
                .mapOrFail(d =>
                    d.asScala match
                        case d: FiniteDuration => Right(Timeout(d))
                        case _ => Left(Config.Error.InvalidData(message = "timeout must be finite"))
                )
        ).nested("akka").map(AkkaConfig.apply)
    end config
end AkkaConfig
