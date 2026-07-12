// PURPOSE: Test-support harness asserting an EngineDefinition's declared shape matches its real
// PURPOSE: classify/emit/fold behavior — guards against declaration/implementation drift, test-only
package works.iterative.workflow

/** A witness pins one `(transition, outcome)` edge of an `EngineDefinition` to a concrete `(entity,
  * command)` pair so `ConformanceCheck.errors` can exercise the real `classify`/`emit`/ fold code
  * against the declared shape.
  *
  * @param key
  *   the `(from, on.label)` transition key this witness targets
  * @param outcomeLabel
  *   the declared outcome label expected to be selected by `classify`
  * @param entity
  *   the entity value to run the witness against
  * @param command
  *   the command value to run the witness against
  */
final case class Witness[State, Entity, Command](
    key: (State, String),
    outcomeLabel: String,
    entity: Entity,
    command: Command
)

object ConformanceCheck:

    /** Enumerates every `(transition, outcome)` in `defn` and asserts declaration ≡ real code:
      *
      *   1. Missing-witness = failure by construction — any enumerated edge with zero witnesses is
      *      reported as an error (adding an outcome without a witness fails the suite).
      *   1. Per witness, against the real code:
      *      - `t.classify(entity, command).label == outcomeLabel`;
      *      - SET-conformance of `emit`: every emitted event matches some declared `EventShape` and
      *        every declared shape is produced (cardinality is deliberately unchecked — emit
      *        fan-out is data-dependent);
      *      - fold: `emits.foldLeft(Right(entity))(_ flatMap foldEvent(_, _))` lands on an entity
      *        whose `defn.stateOf` equals the outcome's declared `to`.
      *
      * Returns `Nil` when the definition and its witnesses are conformant.
      */
    def errors[R, State, Entity, Command, Event](
        defn: EngineDefinition[R, State, Entity, Command, Event],
        foldEvent: (Entity, Event) => Either[Any, Entity],
        witnesses: List[Witness[State, Entity, Command]]
    ): List[String] =
        val witnessesByEdge = witnesses.groupBy(w => (w.key, w.outcomeLabel))

        defn.transitions.flatMap { t =>
            t.outcomes.flatMap { o =>
                witnessesByEdge.getOrElse((t.key, o.label), Nil) match
                    case Nil => List(s"no witness for transition ${t.key} outcome '${o.label}'")
                    case ws  => ws.flatMap(w => checkWitness(defn.stateOf, t, o, w, foldEvent))
            }
        }
    end errors

    private def checkWitness[R, State, Entity, Command, Event](
        stateOf: Entity => State,
        t: Transition[R, State, Entity, Command, Event],
        o: Outcome[State, Entity, Command, Event],
        w: Witness[State, Entity, Command],
        foldEvent: (Entity, Event) => Either[Any, Entity]
    ): List[String] =
        val edge = s"${t.key} / outcome '${o.label}'"

        val classifyErrors =
            val actualLabel = t.classify(w.entity, w.command).label
            if actualLabel == w.outcomeLabel then Nil
            else
                List(s"$edge: classify returned label '$actualLabel', expected '${w.outcomeLabel}'")
        end classifyErrors

        val emitted = o.emit(w.entity, w.command)
        val unmatchedEmitted = emitted.filterNot(e => o.emits.exists(_.matches(e)))
        val unproducedShapes = o.emits.filterNot(shape => emitted.exists(shape.matches))
        val setErrors =
            (if unmatchedEmitted.isEmpty then Nil
             else List(s"$edge: emit produced event(s) matching no declared EventShape")) :::
                (if unproducedShapes.isEmpty then Nil
                 else
                     List(
                         s"$edge: declared EventShape(s) [${unproducedShapes.map(_.label).mkString(", ")}] were not produced"
                     )
                )

        val foldErrors =
            val folded = emitted.foldLeft[Either[Any, Entity]](Right(w.entity)) { (acc, e) =>
                acc.flatMap(entity => foldEvent(entity, e))
            }
            folded match
                case Right(end) if stateOf(end) == o.to => Nil
                case Right(end) =>
                    List(s"$edge: fold landed on state '${stateOf(end)}', expected '${o.to}'")
                case Left(err) => List(s"$edge: fold failed: $err")
            end match
        end foldErrors

        classifyErrors ::: setErrors ::: foldErrors
    end checkWitness
end ConformanceCheck
