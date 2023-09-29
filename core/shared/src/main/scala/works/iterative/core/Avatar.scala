package works.iterative.core

import java.net.URI

// TODO: validate URL - or should we go with URI?
opaque type Avatar = String

object Avatar extends ValidatedStringFactory[Avatar](a => a):
  def apply(avatar: String): Validated[Avatar] =
    Validated.nonEmptyString("avatar")(avatar)
  def apply(avatar: URI): Validated[Avatar] =
    Validated.nonNull("avatar")(avatar).flatMap(a => apply(a.toString))

  extension (a: Avatar) def url: String = a
