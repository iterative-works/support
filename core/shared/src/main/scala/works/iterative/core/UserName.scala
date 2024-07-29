package works.iterative.core

import works.iterative.core.auth.UserId

// Full name of the user
opaque type UserName = String

object UserName extends ValidatedStringFactory[UserName](u => u):
    def apply(value: String): Validated[UserName] =
        // Validate that the value is not empty
        Validated.nonEmptyString("user.name")(value)

    def system(value: UserId): UserName = UserName.unsafe(s"#${value.value}")

    extension (u: UserName)
        def display(using messages: MessageCatalogue): String =
            if u.startsWith("#") then
                val userId = u.drop(1)
                messages.get(s"user.system.${userId}").getOrElse(userId)
            else u
end UserName
