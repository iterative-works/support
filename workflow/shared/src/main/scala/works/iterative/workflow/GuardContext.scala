// PURPOSE: Defines GuardContext — the calling user's identity and entity state for guard evaluation
// PURPOSE: Carries roles, userId, and the entity; passed to Guard.check
package works.iterative.workflow

import works.iterative.core.auth.UserId

/** The context available during guard evaluation.
  *
  * @param roles
  *   the full set of roles held by the calling user, typed as `R` — the application-supplied role
  *   type.
  * @param userId
  *   the identity of the calling user (canonical `idDO`-based UserId)
  * @param entity
  *   the current entity state being guarded
  */
final case class GuardContext[R, S](roles: Set[R], userId: UserId, entity: S)
