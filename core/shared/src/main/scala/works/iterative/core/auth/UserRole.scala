package works.iterative.core
package auth

opaque type UserRole = String

object UserRole extends ValidatedStringFactory[UserRole](r => r):
    def apply(role: String): Validated[UserRole] =
        Validated.nonEmptyString("user.role")(role)

    val Klient = UserRole.unsafe("Klient")
end UserRole
