// PURPOSE: Defines permission inheritance rules for different resource namespaces
// PURPOSE: Enables Zanzibar-style computed permissions where relations can imply other relations

package works.iterative.core.auth

/** Configuration for permission inheritance within a specific namespace.
  *
  * Defines which relations imply other relations. For example, an "owner" relation might imply
  * "editor" and "viewer" relations.
  *
  * @param implications
  *   Map from relation to the set of relations it implies
  *
  * Example:
  * {{{
  *   NamespaceConfig(
  *     implications = Map(
  *       "owner" -> Set("edit", "view", "delete"),
  *       "editor" -> Set("edit", "view"),
  *       "viewer" -> Set("view")
  *     )
  *   )
  * }}}
  */
case class NamespaceConfig(
    implications: Map[String, Set[String]]
)

/** Global permission configuration defining inheritance rules for all namespaces.
  *
  * This configuration is used by the permission system to compute which permissions are granted
  * based on stored relation tuples. It implements a Zanzibar-inspired model where permissions can
  * be derived through relation inheritance.
  *
  * @param namespaces
  *   Map from namespace name to its configuration
  *
  * Example:
  * {{{
  *   PermissionConfig(
  *     namespaces = Map(
  *       "document" -> NamespaceConfig(
  *         implications = Map(
  *           "owner" -> Set("edit", "view", "delete"),
  *           "editor" -> Set("edit", "view")
  *         )
  *       ),
  *       "folder" -> NamespaceConfig(
  *         implications = Map(
  *           "owner" -> Set("manage", "view")
  *         )
  *       )
  *     )
  *   )
  * }}}
  */
case class PermissionConfig(
    namespaces: Map[String, NamespaceConfig]
):
    /** Maximum depth for permission inheritance traversal. This prevents DoS attacks from deeply
      * nested or circular permission hierarchies.
      */
    private val maxInheritanceDepth = 10

    /** Computes all relations that are granted by a given relation in a namespace.
      *
      * This includes:
      *   - The direct relation itself
      *   - All relations directly implied by it
      *   - All relations transitively implied (up to maxInheritanceDepth)
      *
      * If the namespace is unknown, returns only the direct relation.
      *
      * @param namespace
      *   The resource namespace (e.g., "document", "folder")
      * @param relation
      *   The starting relation (e.g., "owner", "editor")
      * @return
      *   Set of all relations granted by the input relation
      *
      * Example:
      * {{{
      *   // If owner -> editor -> viewer
      *   config.computedRelations("document", "owner")
      *   // Returns: Set("owner", "editor", "viewer")
      * }}}
      */
    def computedRelations(namespace: String, relation: String): Set[String] =
        namespaces.get(namespace) match
            case None =>
                // Unknown namespace - return only the direct relation
                Set(relation)
            case Some(nsConfig) =>
                // Compute transitive closure of implications with depth limit
                computeTransitiveClosure(relation, nsConfig.implications, maxInheritanceDepth)

    /** Computes the transitive closure of implications starting from a relation.
      *
      * Uses breadth-first traversal to find all implied relations, respecting the maximum depth
      * limit to prevent excessive computation.
      *
      * @param start
      *   The starting relation
      * @param implications
      *   The implication map for this namespace
      * @param maxDepth
      *   Maximum depth to traverse
      * @return
      *   Set of all relations reachable from start (including start itself)
      */
    private def computeTransitiveClosure(
        start: String,
        implications: Map[String, Set[String]],
        maxDepth: Int
    ): Set[String] =
        @scala.annotation.tailrec
        def traverse(
            current: Set[String],
            visited: Set[String],
            depth: Int
        ): Set[String] =
            if depth >= maxDepth || current.isEmpty then
                visited
            else
                // Find all relations implied by current set
                val implied = current.flatMap(rel => implications.getOrElse(rel, Set.empty))
                // Only process relations we haven't seen yet
                val newRelations = implied -- visited
                traverse(newRelations, visited ++ newRelations, depth + 1)

        // Start with the initial relation in the visited set
        traverse(Set(start), Set(start), 0)
    end computeTransitiveClosure
end PermissionConfig

object PermissionConfig:
    /** Default configuration with common document and folder permissions.
      *
      * Document namespace:
      *   - owner: can delete, edit, and view
      *   - editor: can edit and view
      *   - viewer: can view
      *
      * Folder namespace:
      *   - owner: can manage and view contents
      *   - member: can view contents
      */
    val default: PermissionConfig = PermissionConfig(
        namespaces = Map(
            "document" -> NamespaceConfig(
                implications = Map(
                    "owner" -> Set("edit", "view", "delete"),
                    "editor" -> Set("edit", "view"),
                    "viewer" -> Set("view")
                )
            ),
            "folder" -> NamespaceConfig(
                implications = Map(
                    "owner" -> Set("manage", "view"),
                    "member" -> Set("view")
                )
            )
        )
    )
end PermissionConfig
