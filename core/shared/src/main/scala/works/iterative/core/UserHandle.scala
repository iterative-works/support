package works.iterative.core

/** A simple object to hold unique user identification together with "humane"
  * identification Usually we need to display the user's name in the UI, which
  * we should be able to do without resorting to external services. This handle
  * is to be used in all user-generated content, like comments, reviews, etc.
  */
final case class UserHandle(
    userId: UserId,
    userName: UserName
)

object UserHandle:
  def apply(userId: String, userName: String): Validated[UserHandle] =
    for
      id <- UserId(userId)
      name <- UserName(userName)
    yield UserHandle(id, name)

  def unsafe(userId: String, userName: String): UserHandle =
    UserHandle(UserId.unsafe(userId), UserName.unsafe(userName))
