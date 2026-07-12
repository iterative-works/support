// PURPOSE: Defines GuardFailure — the sum-aware failure algebra for Guard.check
// PURPOSE: Unauthorized is a distinct case, distinguishable even inside Conjunction/Disjunction
package works.iterative.workflow

import works.iterative.core.UserMessage

/** The result of a failed guard evaluation.
  *
  * Sum-aware: carries structural information about which guards failed and why. `Unauthorized` is
  * intentionally a distinct case from `Leaf` so that callers (e.g. MEDECA-377, MEDECA-378) can
  * distinguish "missing data" from "missing permission" without further refactoring.
  *
  *   - `Leaf` — a data guard (`Requirement`) failed; carries the predicate label and message key
  *   - `Unauthorized` — an auth guard (`RequireRole` or `RequireIdentity`) failed
  *   - `Conjunction` — an `And` guard: all failing branches accumulated (flat, no nesting)
  *   - `Disjunction` — an `Or` guard: all branches failed (node-level, two elements)
  */
enum GuardFailure:
    case Leaf(label: String, message: UserMessage)
    case Unauthorized(label: String)
    case Conjunction(failures: List[GuardFailure])
    case Disjunction(failures: List[GuardFailure])
end GuardFailure
