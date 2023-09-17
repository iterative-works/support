package works.iterative.tapir

import zio.*

trait BaseUriPlatformSpecific:
  def fromLocation: BaseUri = BaseUri(
    org.scalajs.dom.window.location.href
      .split("/")
      .dropRight(1)
      .mkString("/")
  )

  def fromLocation(upTo: String): BaseUri = BaseUri(
    org.scalajs.dom.window.location.href
      .split("/")
      .takeWhile(_ != upTo)
      .mkString("/")
  )

  val layer: ULayer[BaseUri] = ZLayer.succeed(fromLocation)
  def layer(upTo: String): ULayer[BaseUri] =
    ZLayer.succeed(fromLocation(upTo))
