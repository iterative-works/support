// PURPOSE: Represents a relation tuple in the Zanzibar-inspired permission model
// PURPOSE: Models (user, relation, target) triples that define who has what relationship with which resource

package works.iterative.core.auth

/**
 * A relation tuple represents a relationship between a user and a target resource.
 *
 * In Zanzibar-inspired ReBAC (Relationship-Based Access Control), permissions are
 * derived from relation tuples. Each tuple states that a specific user has a specific
 * relationship with a target resource.
 *
 * @param user The user who has the relation (subject)
 * @param relation The type of relationship (e.g., "owner", "editor", "viewer")
 * @param target The resource that the relation applies to (object)
 *
 * Example:
 * {{{
 *   val tuple = RelationTuple(
 *     UserId.unsafe("user123"),
 *     "owner",
 *     PermissionTarget.unsafe("document", "doc456")
 *   )
 * }}}
 *
 * This states: "user123 is an owner of document:doc456"
 */
case class RelationTuple(
    user: UserId,
    relation: String,
    target: PermissionTarget
)
