package works.iterative.core.auth

final case class CurrentUser(userProfile: BasicProfile) extends UserProfile:
  export userProfile.*
