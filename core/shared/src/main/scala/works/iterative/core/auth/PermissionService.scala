package works.iterative.core.auth

import zio.*
import works.iterative.core.Validated
import zio.prelude.Validation
import works.iterative.core.UserMessage

opaque type PermissionOp = String
object PermissionOp:
  def apply(op: String): PermissionOp = op

  extension (op: PermissionOp) def value: String = op

trait Targetable:
  def permissionTarget: PermissionTarget

opaque type PermissionTarget = String
object PermissionTarget:
  given Conversion[Targetable, PermissionTarget] = _.permissionTarget

  def apply(target: String): Validated[PermissionTarget] =
    target.split(":", 2) match
      case Array(n, i) =>
        apply(
          n,
          i.split("#", 2).head,
          if i.contains("#") then i.split("#").lastOption else None
        )
      case _ => Validation.fail(UserMessage("error.target.format"))

  def apply(namespace: String, id: String): Validated[PermissionTarget] =
    apply(namespace, id, None)

  def apply(
      namespace: String,
      id: String,
      rel: Option[String]
  ): Validated[PermissionTarget] =
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
    yield
      val v = s"$n:$i"
      rel.fold(v)(r => s"${v}#${r}")

  def unsafe(target: String): PermissionTarget =
    require(target.trim.nonEmpty, "Target must be defined")
    require(target.indexOf(':') != -1, "Target must contain ':'")
    target

  def unsafe(
      namespace: String,
      id: String,
      rel: String
  ): PermissionTarget = unsafe(namespace, id, Some(rel))

  def unsafe(
      namespace: String,
      id: String,
      rel: Option[String] = None
  ): PermissionTarget =
    require(
      namespace.trim.nonEmpty && id.trim.nonEmpty,
      "Both namespace and id must be defined"
    )
    // TODO: escape instead of complaining
    require(
      namespace.indexOf(':') == -1 && id.indexOf(':') == -1,
      "Neither namespace nor id can contain ':'"
    )
    val v = s"${namespace}:${id}"
    rel.fold(v)(r => s"${v}#${r}")

  extension (target: PermissionTarget)
    def namespace: String = target.split(":", 2).head
    def value: String = target.split(":", 2).last.takeWhile(_ != '#')
    def rel: Option[String] =
      if target.contains("#") then target.split("#", 2).lastOption
      else None

trait PermissionService:
  def isAllowed(
      subj: Option[UserInfo],
      action: PermissionOp,
      obj: PermissionTarget
  ): UIO[Boolean]

  def isAllowed(
      subj: UserInfo,
      action: PermissionOp,
      obj: PermissionTarget
  ): UIO[Boolean] = isAllowed(Some(subj), action, obj)

object PermissionService:
  def isAllowed(
      subj: Option[UserInfo],
      action: PermissionOp,
      obj: PermissionTarget
  ): URIO[PermissionService, Boolean] =
    ZIO.serviceWithZIO(_.isAllowed(subj, action, obj))

  def isAllowed(
      subj: UserInfo,
      action: PermissionOp,
      obj: PermissionTarget
  ): URIO[PermissionService, Boolean] =
    ZIO.serviceWithZIO(_.isAllowed(subj, action, obj))
