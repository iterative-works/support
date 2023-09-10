package works.iterative.core.auth

import zio.*
import works.iterative.core.Validated
import zio.prelude.Validation
import works.iterative.core.UserMessage

opaque type PermissionOp = String
object PermissionOp:
  def apply(op: String): PermissionOp = op

  extension (op: PermissionOp) def value: String = op

opaque type PermissionTarget = String
object PermissionTarget:
  def apply(target: String): Validated[PermissionTarget] =
    target.split(":", 2) match
      case Array(n, i) => apply(n, i)
      case _           => Validation.fail(UserMessage("error.target.format"))

  def apply(namespace: String, id: String): Validated[PermissionTarget] =
    for
      n <- Validation.fromPredicateWith(UserMessage("error.namespace.empty"))(
        namespace
      )(_.trim.nonEmpty)
      i <- Validation.fromPredicateWith(UserMessage("error.id.empty"))(id)(
        _.trim.nonEmpty
      )
      _ <- Validation.fromPredicateWith(UserMessage("error.namespace.colon"))(
        n
      )(_.indexOf(':') == -1)
      _ <- Validation.fromPredicateWith(UserMessage("error.id.colon"))(i)(
        _.indexOf(':') == -1
      )
    yield s"$n:$i"

  def unsafe(target: String): PermissionTarget =
    require(target.trim.nonEmpty, "Target must be defined")
    require(target.indexOf(':') != -1, "Target must contain ':'")
    target

  def unsafe(namespace: String, id: String): PermissionTarget =
    require(
      namespace.trim.nonEmpty && id.trim.nonEmpty,
      "Both namespace and id must be defined"
    )
    require(
      namespace.indexOf(':') == -1 && id.indexOf(':') == -1,
      "Neither namespace nor id can contain ':'"
    )
    s"${namespace}:${id}"

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
