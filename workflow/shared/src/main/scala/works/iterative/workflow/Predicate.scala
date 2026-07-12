// PURPOSE: Defines Predicate — a named boolean test on an entity state
// PURPOSE: Used by Guard leaves to express data conditions without coupling to aggregates
package works.iterative.workflow

/** A named predicate over entity state S.
  *
  * @param label
  *   stable, language-independent identifier; used by `Guard.describe` for diagram export (Phase 4)
  * @param test
  *   pure boolean function applied to the entity value
  */
final case class Predicate[S](label: String, test: S => Boolean):
    def apply(s: S): Boolean = test(s)
end Predicate
