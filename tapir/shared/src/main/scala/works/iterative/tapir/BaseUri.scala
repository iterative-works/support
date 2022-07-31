package works.iterative.tapir

import sttp.model.Uri

case class BaseUri(value: Option[Uri])

object BaseUri:

  def apply(optU: Option[Uri]): BaseUri = BaseUri(optU)
  def apply(u: Uri): BaseUri = BaseUri(Some(u))

  extension (v: BaseUri) def toUri: Option[Uri] = v.value
