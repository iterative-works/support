package works.iterative.tapir

import sttp.model.Uri
import sttp.model.Uri.*
import sttp.capabilities.zio.ZioStreams

case class BaseUri(value: Option[Uri])

object BaseUri extends BaseUriPlatformSpecific:
  def apply(s: String): BaseUri = BaseUri(Some(uri"$s"))
  def apply(u: Uri): BaseUri = BaseUri(Some(u))
  extension (v: BaseUri)
    def toUri: Option[Uri] = v.value
    def toWSUri: Option[Uri] = v.value.map(u =>
      u.scheme match
        case Some("https") => u.scheme("wss")
        case _             => u.scheme("ws")
    )
    def /(s: String): BaseUri = v.value match
      case Some(u) => BaseUri(Some(uri"$u/$s"))
      case None    => BaseUri(Some(uri"$s"))
    def href: String = v.value match
      case Some(u) => u.toString
      case None    => "#"

trait BaseUriExtractor[-O]:
  def extractBaseUri(using baseUri: BaseUri): Option[Uri]

object BaseUriExtractor extends LowPriorityBaseUriImplicits:
  given extractWSBaseUri[A, B]: BaseUriExtractor[ZioStreams.Pipe[A, B]] with
    def extractBaseUri(using baseUri: BaseUri): Option[Uri] = baseUri.toWSUri

trait LowPriorityBaseUriImplicits:
  given extractBaseUri: BaseUriExtractor[Any] with
    def extractBaseUri(using baseUri: BaseUri): Option[Uri] = baseUri.toUri
