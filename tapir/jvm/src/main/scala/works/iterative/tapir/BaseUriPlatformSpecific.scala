package works.iterative.tapir

import zio.*
import zio.config.*
import sttp.model.Uri.*

trait BaseUriPlatformSpecific:
  val configDesc: ConfigDescriptor[BaseUri] =
    import ConfigDescriptor.*
    string("BASEURI").optional.map(_.map(s => uri"$s")).to[BaseUri]

  val fromEnv: ZLayer[Any, ReadError[String], BaseUri] =
    ZConfig.fromSystemEnv(
      configDesc,
      keyDelimiter = Some('_'),
      valueDelimiter = Some(',')
    )
