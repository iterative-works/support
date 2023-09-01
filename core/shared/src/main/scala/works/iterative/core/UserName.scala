package works.iterative.core

// Full name of the user
opaque type UserName = String

object UserName:
  def apply(value: String): Validated[UserName] =
    // Validate that the value is not empty
    Validated.nonEmptyString("user.name")(value)

  def unsafe(value: String): UserName = value
