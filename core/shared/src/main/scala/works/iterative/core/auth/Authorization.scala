// PURPOSE: ZIO-based authorization helpers providing declarative permission guards for application layer
// PURPOSE: Orchestrates effects (CurrentUser + PermissionService); pure authorization rules live in PermissionLogic

package works.iterative.core.auth

import zio.*

/** Authorization helper object providing declarative guards for permission checking.
  *
  * NOTE: This is application-layer infrastructure that orchestrates effects. Pure authorization
  * domain logic lives in PermissionLogic.
  *
  * Usage patterns:
  * {{{
  *   // Require permission or fail with Forbidden
  *   Authorization.require(PermissionOp("edit"), document.target) {
  *     documentService.update(document)
  *   }
  *
  *   // Check permission without failing
  *   Authorization.check(PermissionOp("delete"), document.target).flatMap { allowed =>
  *     if (allowed) showDeleteButton else ZIO.unit
  *   }
  *
  *   // Filter result based on permission
  *   Authorization.withPermission(PermissionOp("view"), document.target) {
  *     documentRepository.findById(id)
  *   }
  *
  *   // Filter list of resources by permission
  *   Authorization.filterAllowed(PermissionOp("view"), documents)(_.target)
  * }}}
  */
object Authorization:

    /** Require permission before executing effect, fail with Forbidden if denied.
      *
      * This method enforces fail-closed behavior: if permission check fails or returns false, the
      * effect is not executed and an AuthenticationError.Forbidden is raised.
      *
      * @param op
      *   The permission operation to check
      * @param target
      *   The target resource
      * @param effect
      *   The effect to execute if permission granted
      * @return
      *   ZIO effect that succeeds only if permission granted
      */
    def require[R, E, A](
        op: PermissionOp,
        target: PermissionTarget
    )(effect: ZIO[R, E, A]): ZIO[R & CurrentUser & PermissionService, E | AuthenticationError, A] =
        for
            currentUser <- ZIO.service[CurrentUser]
            permService <- ZIO.service[PermissionService]
            allowed <- permService.isAllowed(currentUser, op, target)
            result <- if allowed then effect
            else ZIO.fail(AuthenticationError.Forbidden(target.value, op.value))
        yield result

    /** Check if current user has permission on target resource.
      *
      * This method returns a Boolean indicating whether permission is granted. Unlike `require`,
      * this does not fail - it returns false when permission is denied.
      *
      * Note: PermissionService.isAllowed returns UIO[Boolean] (cannot fail), so this method also
      * cannot fail. The typed error channel provides future extensibility if we need to add
      * fallible permission checks.
      *
      * @param op
      *   The permission operation to check
      * @param target
      *   The target resource
      * @return
      *   ZIO[CurrentUser & PermissionService, AuthenticationError, Boolean]
      */
    def check(
        op: PermissionOp,
        target: PermissionTarget
    ): ZIO[CurrentUser & PermissionService, AuthenticationError, Boolean] =
        for
            currentUser <- ZIO.service[CurrentUser]
            permService <- ZIO.service[PermissionService]
            allowed <- permService.isAllowed(currentUser, op, target)
        yield allowed

    /** Execute effect only if permission granted, return None if denied.
      *
      * This method filters the effect result based on permission. If permission is denied, it
      * returns None without executing the effect. If permission is granted, it executes the effect
      * and returns its result.
      *
      * @param op
      *   The permission operation to check
      * @param target
      *   The target resource
      * @param effect
      *   The effect that returns Option[A]
      * @return
      *   ZIO effect that returns None if permission denied, otherwise effect result
      */
    def withPermission[R, E, A](
        op: PermissionOp,
        target: PermissionTarget
    )(effect: ZIO[R, E, Option[A]]): ZIO[R & CurrentUser & PermissionService, E, Option[A]] =
        for
            currentUser <- ZIO.service[CurrentUser]
            permService <- ZIO.service[PermissionService]
            allowed <- permService.isAllowed(currentUser, op, target)
            result <- if allowed then effect else ZIO.succeed(None)
        yield result

    /** Filter a list of resources to only those the current user has permission to access.
      *
      * This method uses a batch query (PermissionService.listAllowed) for efficient filtering.
      * Instead of checking permission N times (once per resource), it makes a single query to get
      * all allowed resource IDs, then filters the list locally.
      *
      * This is significantly more efficient for large lists:
      *   - N individual checks: O(N * permission_check_cost)
      *   - Batch query: O(1 * list_allowed_cost + N * filter_cost)
      *
      * @param op
      *   The permission operation to check
      * @param items
      *   The list of resources to filter
      * @param extractTarget
      *   Function to extract PermissionTarget from each resource
      * @return
      *   ZIO effect returning filtered list of permitted resources
      */
    def filterAllowed[A](
        op: PermissionOp,
        items: Seq[A]
    )(extractTarget: A => PermissionTarget): ZIO[CurrentUser & EnumerablePermissionService, Nothing, Seq[A]] =
        for
            currentUser <- ZIO.service[CurrentUser]
            permService <- ZIO.service[EnumerablePermissionService]

            // Group items by namespace for efficient batching
            itemsByNamespace = items.groupBy(item => extractTarget(item).namespace)

            // Get allowed IDs for each namespace (batch query)
            allowedByNamespace <- ZIO.foreach(itemsByNamespace.toSeq) { case (namespace, nsItems) =>
                permService.listAllowed(currentUser, op, namespace)
                    .map(allowedIds => (namespace, allowedIds, nsItems))
            }

            // Filter items to only those in allowed sets
            allowed = allowedByNamespace.flatMap { case (_, allowedIds, nsItems) =>
                nsItems.filter(item => allowedIds.contains(extractTarget(item).value))
            }
        yield allowed

end Authorization
