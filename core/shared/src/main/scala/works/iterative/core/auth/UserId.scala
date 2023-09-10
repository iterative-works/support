package works.iterative.core.auth

import works.iterative.core.Validated

// Unique identifier of the user
opaque type UserId = String

object UserId:
  def apply(value: String): Validated[UserId] =
    // Validate that the value is not empty
    Validated.nonEmptyString("user.id")(value)

  def unsafe(value: String): UserId = value

  extension (u: UserId) def value: String = u
