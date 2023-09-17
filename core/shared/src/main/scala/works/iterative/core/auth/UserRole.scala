package works.iterative.core
package auth

opaque type UserRole = String

object UserRole:
  def apply(role: String): Validated[UserRole] =
    Validated.nonEmptyString("user.role")(role)
  def unsafe(role: String): UserRole = role

  extension (r: UserRole) def value: String = r
