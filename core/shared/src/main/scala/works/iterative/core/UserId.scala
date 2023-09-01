package works.iterative.core

// Unique identifier of the user
opaque type UserId = String

object UserId:
  def apply(value: String): Validated[UserId] =
    // Validate that the value is not empty
    Validated.nonEmptyString("user.id")(value)

  def unsafe(value: String): UserId = value
