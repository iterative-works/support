// PURPOSE: Repository trait for persisting permission relation tuples to SQL database
// PURPOSE: Defines contract for storing and querying user-resource-relation triples

package works.iterative.sqldb

import zio.*
import works.iterative.core.auth.*

/** Repository interface for persisting RelationTuples to a SQL database.
  *
  * This trait defines the contract for storing Zanzibar-inspired permission tuples, enabling
  * database-backed PermissionService implementations.
  *
  * Implementation notes:
  *   - All operations return Task[T] to handle database errors
  *   - Database errors should be handled at the service layer (fail-closed pattern)
  *   - Implementations should use parameterized queries to prevent SQL injection
  *   - addRelation should be idempotent (duplicate inserts should succeed)
  */
trait PermissionRepository:

    /** Check if a specific relation exists between user and target.
      *
      * @param userId
      *   The user ID to check
      * @param relation
      *   The relationship type (e.g., "owner", "editor", "viewer")
      * @param target
      *   The permission target
      * @return
      *   Task[Boolean] - true if relation exists, false otherwise
      */
    def hasRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Boolean]

    /** Add a relation tuple to the database.
      *
      * This operation should be idempotent - adding the same relation twice should succeed without
      * error (either by using INSERT IGNORE or catching unique constraint violations).
      *
      * @param userId
      *   The user ID
      * @param relation
      *   The relationship type
      * @param target
      *   The permission target
      * @return
      *   Task[Unit]
      */
    def addRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Unit]

    /** Remove a relation tuple from the database.
      *
      * This operation should be idempotent - removing a non-existent relation should succeed.
      *
      * @param userId
      *   The user ID
      * @param relation
      *   The relationship type
      * @param target
      *   The permission target
      * @return
      *   Task[Unit]
      */
    def removeRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Unit]

    /** Get all relation tuples for a user in a specific namespace.
      *
      * This is the primary query for permission checking - fetches all relations the user has
      * within a namespace, which are then evaluated by PermissionLogic.
      *
      * @param userId
      *   The user ID
      * @param namespace
      *   The resource namespace to filter by
      * @return
      *   Task[Set[RelationTuple]] - Set of all relation tuples for the user in namespace
      */
    def getUserRelations(
        userId: UserId,
        namespace: String
    ): Task[Set[RelationTuple]]

end PermissionRepository
