// PURPOSE: Pure domain logic for permission checking (FCIS functional core)
// PURPOSE: Provides testable, effect-free functions for computing access permissions

package works.iterative.core.auth

/**
 * Pure domain logic for permission checking.
 *
 * This object contains the functional core of the permission system following
 * FCIS (Functional Core, Imperative Shell) principles. All functions here are:
 * - Pure (no side effects)
 * - Deterministic (same inputs always produce same outputs)
 * - Testable without ZIO or any infrastructure
 *
 * The effect-based orchestration (ZIO, database access, etc.) belongs in
 * PermissionService implementations, not here.
 */
object PermissionLogic:

  /**
   * Checks if a user is allowed to perform an action on a target resource.
   *
   * This is a pure function that determines permission by:
   * 1. Finding all relation tuples for the user and target
   * 2. For each relation, computing all implied permissions using the config
   * 3. Checking if the requested action is in the set of granted permissions
   *
   * @param userId The user requesting access
   * @param action The permission being checked (e.g., "view", "edit", "delete")
   * @param target The target resource (e.g., "document:123")
   * @param tuples All relation tuples in the system
   * @param config Permission configuration defining inheritance rules
   * @return true if the user is allowed, false otherwise
   *
   * Example:
   * {{{
   *   val tuples = Set(
   *     RelationTuple(userId, "owner", target)
   *   )
   *   val config = PermissionConfig.default
   *
   *   // Returns true because owner implies view
   *   PermissionLogic.isAllowed(userId, PermissionOp("view"), target, tuples, config)
   * }}}
   */
  def isAllowed(
      userId: UserId,
      action: PermissionOp,
      target: PermissionTarget,
      tuples: Set[RelationTuple],
      config: PermissionConfig
  ): Boolean =
    // Find all tuples where this user has any relation to this target
    val userTuples = tuples.filter(t => t.user == userId && t.target == target)

    // For each relation the user has, compute all implied permissions
    val grantedPermissions = userTuples.flatMap { tuple =>
      config.computedRelations(target.namespace, tuple.relation)
    }

    // Check if the requested action is in the set of granted permissions
    grantedPermissions.contains(action.value)

  /**
   * Lists all object IDs in a namespace that a user can access with a given action.
   *
   * This is a pure function that performs a reverse lookup: given a user and an action,
   * find all resources they can access. This is essential for efficient authorization-aware
   * queries (e.g., "list all documents this user can edit").
   *
   * @param userId The user whose access we're checking
   * @param action The permission being checked (e.g., "view", "edit")
   * @param namespace The resource namespace to search (e.g., "document", "folder")
   * @param tuples All relation tuples in the system
   * @param config Permission configuration defining inheritance rules
   * @return Set of object IDs the user can access
   *
   * Example:
   * {{{
   *   val tuples = Set(
   *     RelationTuple(userId, "owner", PermissionTarget.unsafe("document", "doc1")),
   *     RelationTuple(userId, "viewer", PermissionTarget.unsafe("document", "doc2"))
   *   )
   *
   *   // Returns Set("doc1", "doc2") because owner and viewer both imply view
   *   PermissionLogic.listAllowed(userId, PermissionOp("view"), "document", tuples, config)
   * }}}
   */
  def listAllowed(
      userId: UserId,
      action: PermissionOp,
      namespace: String,
      tuples: Set[RelationTuple],
      config: PermissionConfig
  ): Set[String] =
    // Find all tuples for this user in this namespace
    val userTuples = tuples.filter(t =>
      t.user == userId && t.target.namespace == namespace
    )

    // For each tuple, check if the relation implies the requested action
    userTuples
      .filter { tuple =>
        val grantedPermissions = config.computedRelations(namespace, tuple.relation)
        grantedPermissions.contains(action.value)
      }
      .map(_.target.value)  // Extract the object ID from the target

end PermissionLogic
