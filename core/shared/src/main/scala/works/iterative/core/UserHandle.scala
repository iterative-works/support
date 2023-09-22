package works.iterative.core
import auth.UserId
import works.iterative.core.auth.CurrentUser

/** A simple object to hold unique user identification together with "humane"
  * identification Usually we need to display the user's name in the UI, which
  * we should be able to do without resorting to external services. This handle
  * is to be used in all user-generated content, like comments, reviews, etc.
  */
final case class UserHandle(
    userId: UserId,
    userName: Option[UserName]
):
  val displayName: String = userName.map(_.value).getOrElse(userId.value)

object UserHandle:

  given userHandleFromCurrentUser(using u: CurrentUser): UserHandle = u.handle

  def apply(userId: String): Validated[UserHandle] =
    for id <- UserId(userId)
    yield UserHandle(id, None)

  def apply(userId: String, userName: String): Validated[UserHandle] =
    for
      id <- UserId(userId)
      name <- UserName(userName)
    yield UserHandle(id, Some(name))

  def unsafe(userId: String): UserHandle =
    UserHandle(UserId.unsafe(userId), None)

  def unsafe(userId: String, userName: String): UserHandle =
    UserHandle(UserId.unsafe(userId), Some(UserName.unsafe(userName)))
