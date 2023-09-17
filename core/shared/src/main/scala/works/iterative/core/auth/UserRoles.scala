package works.iterative.core.auth

trait UserRoles extends UserInfo:
  def roles: Set[UserRole]
