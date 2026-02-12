package works.iterative.core.auth

import zio.*
import works.iterative.core.Validated
import zio.prelude.Validation
import works.iterative.core.UserMessage

opaque type PermissionOp = String
object PermissionOp:
    def apply(op: String): Validated[PermissionOp] =
        Validation.fromPredicateWith(UserMessage("error.permission.op.empty"))(op)(
            _.trim.nonEmpty
        )

    def unsafe(op: String): PermissionOp =
        require(op.trim.nonEmpty, "Permission operation must be defined")
        op

    def unapply(op: PermissionOp): Option[String] = Some(op)

    extension (op: PermissionOp) def value: String = op
end PermissionOp

trait Targetable:
    def permissionTarget: PermissionTarget

opaque type PermissionTarget = String
object PermissionTarget:
    given Conversion[Targetable, PermissionTarget] = _.permissionTarget

    def unapply(
        target: PermissionTarget
    ): Option[(String, String, Option[String])] =
        if target.contains(":") then
            Some((target.namespace, target.value, target.rel))
        else None

    def apply(target: String): Validated[PermissionTarget] =
        target.split(":", 2) match
            case Array(n, i) =>
                apply(
                    n,
                    i.split("#", 2).head,
                    if i.contains("#") then i.split("#").lastOption else None
                )
            case _ => Validation.fail(UserMessage("error.target.format", target))

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
        yield
            val v = s"$n:$i"
            rel.fold(v)(r => s"${v}#${r}")

    def unsafe(target: String): PermissionTarget =
        require(target.trim.nonEmpty, "Target must be defined")
        require(target.indexOf(':') != -1, "Target must contain ':'")
        target
    end unsafe

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
        require(
            namespace.indexOf(':') == -1,
            "Namespace cannot contain ':'"
        )
        // Allow colons in id for URLs (e.g., http://example.com)
        val v = s"${namespace}:${id}"
        rel.fold(v)(r => s"${v}#${r}")
    end unsafe

    // Extension methods parse on each call rather than caching.
    // This is acceptable because:
    // - PermissionTarget strings are short (typically < 100 chars)
    // - Split operations are O(n) where n is small
    // - Caching would add memory overhead and complexity
    // - These are not called in tight loops (permission checks are batched)
    extension (target: PermissionTarget)
        def namespace: String = target.split(":", 2).head
        def value: String = target.split(":", 2).last.takeWhile(_ != '#')
        def rel: Option[String] =
            if target.contains("#") then target.split("#", 2).lastOption
            else None
    end extension
end PermissionTarget

/** Permission service interface for checking user permissions on resources.
  *
  * NOTE: This interface is application-layer (uses ZIO effects), not domain. Pure domain logic
  * lives in PermissionLogic.
  *
  * This is the base trait providing only permission checking (isAllowed). Implementations that
  * support additional capabilities should mix in:
  *   - EnumerablePermissionService for reverse-lookup of allowed resource IDs
  *   - MutablePermissionService for granting/revoking permissions
  */
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
end PermissionService

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
end PermissionService

/** Extension for permission services that support reverse-lookup of allowed resource IDs.
  *
  * This capability is needed for efficient authorization-aware queries. Instead of checking
  * permission on each resource individually, callers can get all allowed resource IDs in one call.
  *
  * Not all implementations can support this (e.g., role-based services that don't track
  * per-resource tuples). Callers that need this capability should declare it in their R type.
  */
trait EnumerablePermissionService extends PermissionService:
    /** List all resource IDs in a namespace that the user is allowed to access.
      *
      * @param subj
      *   The user information
      * @param action
      *   The permission operation to check
      * @param namespace
      *   The resource namespace to search
      * @return
      *   UIO[Set[String]] - Set of resource IDs the user can access
      */
    def listAllowed(
        subj: UserInfo,
        action: PermissionOp,
        namespace: String
    ): UIO[Set[String]]
end EnumerablePermissionService

/** Extension for permission services that support granting and revoking permissions.
  *
  * Not all implementations can support mutation (e.g., role-based services where permissions are
  * derived from roles, not stored as tuples). Callers that need to mutate permissions should
  * declare this in their R type.
  */
trait MutablePermissionService extends PermissionService:
    /** Grant a permission relation to a user.
      *
      * @param userId
      *   The user to grant the permission to
      * @param relation
      *   The permission relation (e.g., "owner", "editor", "viewer")
      * @param target
      *   The permission target
      * @return
      *   UIO[Boolean] - true if successful, false if failed
      */
    def grantPermission(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): UIO[Boolean]

    /** Revoke a permission relation from a user.
      *
      * @param userId
      *   The user to revoke the permission from
      * @param relation
      *   The permission relation to revoke
      * @param target
      *   The permission target
      * @return
      *   UIO[Boolean] - true if successful, false if failed
      */
    def revokePermission(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): UIO[Boolean]
end MutablePermissionService
