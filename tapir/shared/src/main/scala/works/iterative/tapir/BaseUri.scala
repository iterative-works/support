package works.iterative.tapir

import sttp.model.Uri

opaque type BaseUri = Option[Uri]

object BaseUri:

  def apply(optU: Option[Uri]): BaseUri = optU
  def apply(u: Uri): BaseUri = Some(u)

  extension (v: BaseUri) def toUri: Option[Uri] = v
