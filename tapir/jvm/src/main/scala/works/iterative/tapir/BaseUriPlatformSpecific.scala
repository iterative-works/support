package works.iterative.tapir

import zio.*
import sttp.model.Uri.*

trait BaseUriPlatformSpecific:
    given config: Config[BaseUri] =
        Config.string("baseuri").optional.map(_.map(s => uri"$s")).map(BaseUri.apply)

    def fromConfig: ZLayer[Any, Config.Error, BaseUri] = ZLayer(ZIO.config[BaseUri](config))
end BaseUriPlatformSpecific
