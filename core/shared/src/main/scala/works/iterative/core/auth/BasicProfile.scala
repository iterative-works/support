package works.iterative.core
package auth

final case class BasicProfile(
    subjectId: UserId,
    userName: Option[UserName],
    email: Option[Email],
    avatar: Option[Avatar],
    roles: Set[UserRole]
) extends UserProfile:
  val handle: UserHandle = UserHandle(subjectId, userName)

object BasicProfile:
  def apply(p: UserProfile): BasicProfile = p match
    case p: BasicProfile => p
    case _ => BasicProfile(p.subjectId, p.userName, p.email, p.avatar, p.roles)
