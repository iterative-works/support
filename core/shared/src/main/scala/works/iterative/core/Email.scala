package works.iterative.core

import zio.prelude.*

opaque type Email = String

object Email:
  def apply(value: String): Validated[Email] =
    // TODO: email validation
    Validation.succeed(value)

  extension (email: Email) def value: String = email
