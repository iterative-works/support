// PURPOSE: Defines GuardDescription — a renderable tree for workflow diagram export
// PURPOSE: Composed structurally by Guard.describe so description cannot contradict evaluation
package works.iterative.workflow

/** Renderable description of a guard structure, consumed by Phase 4 diagram export.
  *
  * Composed structurally from `Guard.describe`: `And` → `All`, `Or` → `Any`, leaves → `Leaf`. This
  * structural correspondence guarantees the description cannot contradict evaluation.
  */
enum GuardDescription:
    case Leaf(label: String)
    case All(parts: List[GuardDescription])
    case Any(parts: List[GuardDescription])
end GuardDescription
