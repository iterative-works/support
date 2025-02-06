package works.iterative.core.auth

case class AccessToken(token: String)

case class AuthedUserInfo(token: AccessToken, profile: BasicProfile) extends UserProfile:
    export profile.*
