package works.iterative.core.auth

import zio.*

trait PermissionService:
  def isAllowed(subj: UserInfo, action: String, obj: String): UIO[Boolean]

object PermissionService:
  def isAllowed(
      subj: UserInfo,
      action: String,
      obj: String
  ): URIO[PermissionService, Boolean] =
    ZIO.serviceWithZIO(_.isAllowed(subj, action, obj))
