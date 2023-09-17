package works.iterative.core
package auth

trait UserProfile extends UserRoles:
  def userName: Option[UserName]
  def email: Option[Email]
  def avatar: Option[Avatar]
