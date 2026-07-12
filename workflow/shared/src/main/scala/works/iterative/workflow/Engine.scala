// PURPOSE: Defines the declarative outcome-based transition model — CommandMatch, EventShape,
// PURPOSE: Outcome, Transition (+ Transition.simple), and EngineDefinition for the workflow kernel
package works.iterative.workflow

/** A named command matcher — "which command" as a named `PartialFunction` matcher.
  *
  * Chosen over string triggers (drift), sample events (fixture drift), and `ClassTag`/`TypeTest`
  * (breaks on parameterless enum cases, of which Poptávka has many). `label` is both the edge label
  * and the projection's trigger key.
  *
  * @param label
  *   stable identifier for this command match; used as the edge label / trigger key
  * @param matcher
  *   partial function defined exactly on the commands this match accepts
  */
final case class CommandMatch[Command](
    label: String,
    private val matcher: PartialFunction[Command, Unit]
):
    /** Total — never throws, even for a command the matcher is not defined at. */
    def accepts(c: Command): Boolean = matcher.isDefinedAt(c)
end CommandMatch

/** The declared event-type skeleton for an outcome.
  *
  * The conformance harness asserts set-equality between an outcome's declared `emits` shapes and
  * what its real `emit` produces.
  *
  * @param label
  *   stable identifier for this event shape
  * @param matcher
  *   partial function defined exactly on the events this shape matches
  */
final case class EventShape[Event](
    label: String,
    private val matcher: PartialFunction[Event, Unit]
):
    /** Total — never throws, even for an event the matcher is not defined at. */
    def matches(e: Event): Boolean = matcher.isDefinedAt(e)
end EventShape

/** One deterministic outcome of a `Transition`.
  *
  * Co-locates the *declared* skeleton (`to` + `emits`) with the *real* `emit` so they change
  * together. `emits = Nil` with `emit = (_, _) => Seq.empty` models a no-op success outcome (e.g.
  * Poptávka's `NavrhniPL` unchanged-VP branch).
  *
  * @param label
  *   unique within its `Transition`; edge label + witness key
  * @param to
  *   declared target state — read by the diagram, verified by conformance
  * @param emits
  *   declared event-type skeleton (`Nil` for a no-op outcome)
  * @param emit
  *   real, data/payload-dependent event construction
  */
final case class Outcome[State, Entity, Command, Event](
    label: String,
    to: State,
    emits: List[EventShape[Event]],
    emit: (Entity, Command) => Seq[Event]
)

/** One `(from, command)` group of a workflow's engine table.
  *
  * Groups outcomes that share a `from` state and a `CommandMatch`. `classify` is total by
  * construction, so its outcomes are disjoint, exhaustive, and order-free — the footgun-free
  * alternative to N sibling guarded transitions with hopefully-disjoint `applicableWhen`.
  * `classify` cannot reject — rejections live entirely in `requires`/`requiresPayload`.
  *
  * @param from
  *   the source state
  * @param on
  *   the command matcher this transition group applies to
  * @param applicableWhen
  *   fail ⇒ try next transition ⇒ eventually `Unhandled`
  * @param requires
  *   fail ⇒ `Invalid(message)`
  * @param requiresPayload
  *   payload-aware `requires`, evaluated against `(entity, command)`; defaults to always-pass. The
  *   one defaulted additive field landed in Phase 2 so Podání (MEDECA-382) never edits a shipped
  *   kernel type.
  * @param outcomes
  *   the declared, disjoint outcomes this transition group can classify to
  * @param classify
  *   total selection of one of `outcomes` given the real `(entity, command)`
  */
final case class Transition[R, State, Entity, Command, Event](
    from: State,
    on: CommandMatch[Command],
    applicableWhen: Guard[R, Entity],
    requires: Guard[R, Entity],
    requiresPayload: (Entity, Command) => Guard[R, Entity] =
        (_: Entity, _: Command) => Guard.always[R, Entity],
    outcomes: List[Outcome[State, Entity, Command, Event]],
    classify: (Entity, Command) => Outcome[State, Entity, Command, Event]
):
    def key: (State, String) = (from, on.label)
end Transition

object Transition:
    /** Terse constructor for the ~80% passthrough / self-loop case — a single outcome labelled
      * `"only"`, so most of an aggregate's engine table is one call per row.
      */
    def simple[R, State, Entity, Command, Event](
        from: State,
        on: CommandMatch[Command],
        to: State,
        emits: List[EventShape[Event]],
        emit: (Entity, Command) => Seq[Event],
        applicableWhen: Guard[R, Entity] = Guard.always[R, Entity],
        requires: Guard[R, Entity] = Guard.always[R, Entity]
    ): Transition[R, State, Entity, Command, Event] =
        val only = Outcome("only", to, emits, emit)
        Transition(
            from,
            on,
            applicableWhen,
            requires,
            (_: Entity, _: Command) => Guard.always[R, Entity],
            List(only),
            (_, _) => only
        )
    end simple
end Transition

/** The runtime definition consumed by `WorkflowInterpreter.decide`.
  *
  * @param stateOf
  *   the entity→state projection (e.g. for Poptávka: `_.stav`)
  * @param transitions
  *   the transition table; order is preserved and load-bearing — first-applicable wins, mirroring
  *   an `orElse` chain
  */
final case class EngineDefinition[R, State, Entity, Command, Event](
    stateOf: Entity => State,
    transitions: List[Transition[R, State, Entity, Command, Event]]
)
