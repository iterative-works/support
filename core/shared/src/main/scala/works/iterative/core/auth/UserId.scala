package works.iterative.core
package auth

// Unique identifier of the user
opaque type UserId = String

object UserId extends ValidatedStringFactory[UserId](u => u):
  def apply(value: String): Validated[UserId] =
    // Validate that the value is not empty
    Validated.nonEmptyString("user.id")(value)

  extension (u: UserId)
    def target: PermissionTarget = PermissionTarget.unsafe("user", u)
