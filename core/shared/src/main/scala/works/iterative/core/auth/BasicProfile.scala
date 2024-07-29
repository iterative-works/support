package works.iterative.core
package auth

enum Claim:
    case StringClaim(name: String, value: String)

final case class BasicProfile(
    subjectId: UserId,
    userName: Option[UserName],
    email: Option[Email],
    avatar: Option[Avatar],
    roles: Set[UserRole],
    claims: Set[Claim] = Set.empty
) extends UserProfile:
    val handle: UserHandle = UserHandle(subjectId, userName)
    def stringClaim(name: String): Option[String] =
        claims.collectFirst { case Claim.StringClaim(n, v) if n == name => v }
end BasicProfile

object BasicProfile:
    def systemProfile(userId: UserId): BasicProfile =
        BasicProfile(
            userId,
            Some(UserName.system(userId)),
            None,
            None,
            Set.empty
        )

    val anonymous: BasicProfile = systemProfile(UserId.anonymous)

    def apply(p: UserProfile): BasicProfile = p match
        case p: BasicProfile => p
        case _ =>
            BasicProfile(
                p.subjectId,
                p.userName,
                p.email,
                p.avatar,
                p.roles,
                p.claims
            )
end BasicProfile
