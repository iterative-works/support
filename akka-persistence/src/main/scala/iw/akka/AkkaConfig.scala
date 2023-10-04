package works.iterative.akka

import akka.util.Timeout
import zio.*
import zio.config.*
import scala.concurrent.duration.*

case class AkkaConfig(timeout: Timeout = Timeout(10.seconds))

object AkkaConfig:
  val configDescriptor: ConfigDescriptor[AkkaConfig] =
    import ConfigDescriptor.*
    nested("AKKA") {
      duration("TIMEOUT")
        .default(10.seconds)
        .transformOrFail(
          d =>
            d match
              case d: FiniteDuration => Right(Timeout(d))
              case _                 => Left("timeout must be finite")
          ,
          t => Right(t.duration)
        )
    }.to[AkkaConfig]

  val fromEnv: ZLayer[Any, ReadError[String], AkkaConfig] =
    ZConfig.fromSystemEnv(configDescriptor, Some('_'), Some(','))
