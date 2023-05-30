package works.iterative.core.auth

import zio.*

opaque type PermissionOp = String
object PermissionOp:
  def apply(op: String): PermissionOp = op

  extension (op: PermissionOp) def value: String = op

opaque type PermissionTarget = String
object PermissionTarget:
  def apply(namespace: String, id: String): PermissionTarget =
    require(
      namespace.trim.nonEmpty && id.trim.nonEmpty,
      "Both namespece and id must be defined"
    )
    require(
      namespace.indexOf(':') == -1 && id.indexOf(':') == -1,
      "Neither namespace nor id can contain ':'"
    )
    s"$namespace:$id"

  extension (target: PermissionTarget)
    def namespace: String = target.split(":", 2).head
    def value: String = target.split(":", 2).last

trait PermissionService:
  def isAllowed(
      subj: Option[UserInfo],
      action: PermissionOp,
      obj: PermissionTarget
  ): UIO[Boolean]

object PermissionService:
  def isAllowed(
      subj: Option[UserInfo],
      action: PermissionOp,
      obj: PermissionTarget
  ): URIO[PermissionService, Boolean] =
    ZIO.serviceWithZIO(_.isAllowed(subj, action, obj))
