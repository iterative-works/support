// PURPOSE: In-memory ReBAC permission service for testing and simple deployments
// PURPOSE: Application-layer infrastructure (effects + storage), delegates to PermissionLogic for domain rules

package works.iterative.core.auth

import zio.*

/** In-memory implementation of PermissionService using ZIO Ref for state management.
  *
  * This implementation is suitable for:
  * - Testing and development
  * - Simple deployments where permission data fits in memory
  * - Prototyping and demonstrations
  *
  * NOTE: This is application-layer infrastructure (uses ZIO effects and manages state).
  * Pure domain logic lives in PermissionLogic.
  *
  * The service delegates all permission checking logic to PermissionLogic, ensuring that
  * business rules are pure and testable independently of the effect system.
  *
  * @param storage Ref holding the set of relation tuples
  * @param config Permission configuration defining namespace-specific inheritance rules
  */
class InMemoryPermissionService(
    storage: Ref[Set[RelationTuple]],
    config: PermissionConfig
) extends PermissionService:

  /** Check if a user is allowed to perform an action on a target resource.
    *
    * This method delegates to PermissionLogic.isAllowed (pure function) for the actual
    * permission checking logic. This maintains separation between effects (this layer)
    * and pure domain logic (PermissionLogic).
    *
    * @param subj The user information (optional)
    * @param action The permission operation to check
    * @param obj The target resource
    * @return UIO[Boolean] - true if allowed, false otherwise (fail-closed)
    */
  def isAllowed(
      subj: Option[UserInfo],
      action: PermissionOp,
      obj: PermissionTarget
  ): UIO[Boolean] =
    subj match
      case None => ZIO.succeed(false) // Unauthenticated users have no permissions
      case Some(user) =>
        storage.get.map { tuples =>
          PermissionLogic.isAllowed(user.subjectId, action, obj, tuples, config)
        }

  /** Add a relation tuple to the permission store.
    *
    * @param userId The user ID
    * @param relation The relationship type (e.g., "owner", "editor", "viewer")
    * @param target The target resource
    * @return UIO[Unit]
    */
  def addRelation(
      userId: UserId,
      relation: String,
      target: PermissionTarget
  ): UIO[Unit] =
    storage.update(_ + RelationTuple(userId, relation, target))

  /** Remove a relation tuple from the permission store.
    *
    * @param userId The user ID
    * @param relation The relationship type
    * @param target The target resource
    * @return UIO[Unit]
    */
  def removeRelation(
      userId: UserId,
      relation: String,
      target: PermissionTarget
  ): UIO[Unit] =
    storage.update(_ - RelationTuple(userId, relation, target))

  /** List all resource IDs in a namespace that the user is allowed to access.
    *
    * This is a reverse lookup operation useful for authorization-aware queries.
    * For example: "list all documents this user can edit".
    *
    * Delegates to PermissionLogic.listAllowed (pure function).
    *
    * @param subj The user information
    * @param action The permission operation
    * @param namespace The resource namespace to search
    * @return UIO[Set[String]] - Set of resource IDs the user can access
    */
  def listAllowed(
      subj: UserInfo,
      action: PermissionOp,
      namespace: String
  ): UIO[Set[String]] =
    storage.get.map { tuples =>
      PermissionLogic.listAllowed(subj.subjectId, action, namespace, tuples, config)
    }

end InMemoryPermissionService

object InMemoryPermissionService:
  /** Create an InMemoryPermissionService with the given configuration.
    *
    * @param config Permission configuration defining namespace rules
    * @return ZIO effect that creates the service
    */
  def make(config: PermissionConfig): UIO[InMemoryPermissionService] =
    for {
      storage <- Ref.make(Set.empty[RelationTuple])
    } yield InMemoryPermissionService(storage, config)

  /** ZLayer factory for InMemoryPermissionService.
    *
    * Uses lazy initialization pattern with ZLayer.fromZIO.
    * Requires PermissionConfig in the environment.
    */
  val layer: ZLayer[PermissionConfig, Nothing, PermissionService] =
    ZLayer.fromZIO {
      for {
        config <- ZIO.service[PermissionConfig]
        service <- make(config)
      } yield service
    }

end InMemoryPermissionService
