package mdr.pdb.app

import zio.*
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
trait JSAppConfig extends js.Object {
  def baseUrl: String = js.native
  def onlineCheckMs: js.UndefOr[Double] = js.native
}

case class AppConfig(
    baseUrl: String,
    onlineCheckMs: Option[Int]
)

object AppConfig {
  @js.native
  @JSImport("website-config/conf.js", JSImport.Default)
  object config extends JSAppConfig

  val layer: ULayer[AppConfig] =
    ZLayer.succeed(
      AppConfig(
        baseUrl = config.baseUrl,
        onlineCheckMs = config.onlineCheckMs.toOption.map(_.toInt)
      )
    )
}
