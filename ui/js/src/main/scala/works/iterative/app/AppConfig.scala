package works.iterative.app

import zio.*
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import works.iterative.tapir.BaseUri

@js.native
trait JSAppConfig extends js.Object:
    def baseUrl: String = js.native

case class AppConfig(baseUrl: String):
    val baseUri: BaseUri = BaseUri(baseUrl)

object AppConfig:
    @js.native
    @JSImport("website-config/conf.js", JSImport.Default)
    object config extends JSAppConfig

    val layer: ULayer[AppConfig] =
        ZLayer.succeed:
            AppConfig(baseUrl = config.baseUrl)
end AppConfig
