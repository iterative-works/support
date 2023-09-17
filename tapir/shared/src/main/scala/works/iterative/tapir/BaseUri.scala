package works.iterative.tapir

import sttp.model.Uri
import sttp.model.Uri.*

case class BaseUri(value: Option[Uri])

object BaseUri extends BaseUriPlatformSpecific:
  def apply(s: String): BaseUri = BaseUri(Some(uri"$s"))
  def apply(u: Uri): BaseUri = BaseUri(Some(u))
  extension (v: BaseUri)
    def toUri: Option[Uri] = v.value
    def /(s: String): BaseUri = v.value match
      case Some(u) => BaseUri(Some(uri"$u/$s"))
      case None    => BaseUri(Some(uri"$s"))
    def href: String = v.value match
      case Some(u) => u.toString
      case None    => "#"
