// PURPOSE: Production PermissionService with database persistence
// PURPOSE: Implements fail-closed error handling where database failures result in access denial

package works.iterative.sqldb

import zio.*
import works.iterative.core.auth.*

/** Database-backed implementation of PermissionService with fail-closed error handling.
  *
  * This implementation provides production-ready permission storage that:
  * - Persists RelationTuples to a SQL database via PermissionRepository
  * - Delegates permission logic to PermissionLogic (pure domain logic)
  * - Implements fail-closed pattern: database errors result in access denial (return false)
  * - Logs infrastructure failures before denying access for debugging
  *
  * SECURITY CRITICAL: Database errors must NEVER bypass authorization checks.
  * The fail-closed pattern ensures that any infrastructure failure (database down,
  * connection timeout, query error) results in access denial rather than granting
  * unintended permissions.
  *
  * @param repository The repository for persisting relation tuples
  * @param config Permission configuration defining inheritance rules
  */
case class DatabasePermissionService(
    repository: PermissionRepository,
    config: PermissionConfig
) extends PermissionService:

  /** Check if a user is allowed to perform an action on a target resource.
    *
    * This method:
    * 1. Fetches all relation tuples for the user in the target's namespace
    * 2. Delegates to PermissionLogic.isAllowed (pure function)
    * 3. Returns false (fail-closed) if any database error occurs
    * 4. Logs warnings when database errors prevent permission checks
    *
    * @param subj The user information (optional)
    * @param action The permission operation to check
    * @param obj The target resource
    * @return UIO[Boolean] - true if allowed, false if denied or error (fail-closed)
    */
  def isAllowed(
      subj: Option[UserInfo],
      action: PermissionOp,
      obj: PermissionTarget
  ): UIO[Boolean] =
    subj match
      case None =>
        // Unauthenticated users have no permissions
        ZIO.succeed(false)

      case Some(user) =>
        (for
          // Fetch all relation tuples for this user in the target's namespace
          tuples <- repository.getUserRelations(user.subjectId, obj.namespace)

          // Delegate to pure domain logic
          result = PermissionLogic.isAllowed(user.subjectId, action, obj, tuples, config)
        yield result)
          .tapError { error =>
            // Log infrastructure failures for debugging
            ZIO.logWarning(
              s"Permission check failed (fail-closed): user=${user.subjectId} action=${action.value} target=$obj error=${error.getMessage}"
            )
          }
          .catchAll { _ =>
            // Fail-closed: any error results in access denial
            ZIO.succeed(false)
          }

  /** List all resource IDs in a namespace that the user is allowed to access.
    *
    * This method fetches all relation tuples for the user in the namespace,
    * then delegates to PermissionLogic.listAllowed to compute accessible resources.
    *
    * On database error, returns empty set (fail-closed).
    *
    * @param subj The user information
    * @param action The permission operation
    * @param namespace The resource namespace to search
    * @return UIO[Set[String]] - Set of accessible resource IDs
    */
  def listAllowed(
      subj: UserInfo,
      action: PermissionOp,
      namespace: String
  ): UIO[Set[String]] =
    (for
      tuples <- repository.getUserRelations(subj.subjectId, namespace)
      result = PermissionLogic.listAllowed(subj.subjectId, action, namespace, tuples, config)
    yield result)
      .tapError { error =>
        ZIO.logWarning(
          s"List allowed failed (fail-closed): user=${subj.subjectId} action=${action.value} namespace=$namespace error=${error.getMessage}"
        )
      }
      .catchAll { _ =>
        // Fail-closed: return empty set on error
        ZIO.succeed(Set.empty[String])
      }

  /** Grant a permission relation to a user.
    *
    * Persists the relation tuple to the database via repository.addRelation.
    * The repository implementation should be idempotent (duplicate grants succeed).
    *
    * @param userId The user to grant the permission to
    * @param relation The permission relation
    * @param target The permission target
    * @return UIO[Boolean] - true if successful, false if failed
    */
  def grantPermission(
      userId: UserId,
      relation: String,
      target: PermissionTarget
  ): UIO[Boolean] =
    repository.addRelation(userId, relation, target)
      .tapError { error =>
        ZIO.logError(
          s"Grant permission failed: user=$userId relation=$relation target=$target error=${error.getMessage}"
        )
      }
      .fold(_ => false, _ => true)

  /** Revoke a permission relation from a user.
    *
    * Removes the relation tuple from the database via repository.removeRelation.
    * The repository implementation should be idempotent (revoking non-existent relations succeeds).
    *
    * @param userId The user to revoke the permission from
    * @param relation The permission relation to revoke
    * @param target The permission target
    * @return UIO[Boolean] - true if successful, false if failed
    */
  def revokePermission(
      userId: UserId,
      relation: String,
      target: PermissionTarget
  ): UIO[Boolean] =
    repository.removeRelation(userId, relation, target)
      .tapError { error =>
        ZIO.logError(
          s"Revoke permission failed: user=$userId relation=$relation target=$target error=${error.getMessage}"
        )
      }
      .fold(_ => false, _ => true)

end DatabasePermissionService

object DatabasePermissionService:
  /** ZLayer factory for DatabasePermissionService.
    *
    * Requires PermissionRepository and PermissionConfig in the environment.
    */
  val layer: URLayer[PermissionRepository & PermissionConfig, PermissionService] =
    ZLayer.fromFunction((repo: PermissionRepository, config: PermissionConfig) =>
      DatabasePermissionService(repo, config)
    )
end DatabasePermissionService
