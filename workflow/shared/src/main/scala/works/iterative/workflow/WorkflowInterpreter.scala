// PURPOSE: Defines the pure three-stage workflow interpreter — match, gate, classify → emit — plus
// PURPOSE: its construction-time auth policy and GuardFailure → UserMessage collapse
package works.iterative.workflow

import works.iterative.core.UserMessage

/** Construction-time authorization policy for `WorkflowInterpreter.decide`.
  *
  * Not a per-call flag: chosen once when the interpreter is wired to an `EngineDefinition`. `Skip`
  * performs no auth enforcement; `Enforce` is reserved for applications that enforce roles.
  */
enum AuthEnforcement:
    case Skip, Enforce
end AuthEnforcement

/** The kernel-local rejection vocabulary returned by `WorkflowInterpreter.decide`.
  *
  * The application's adapter maps this to its own error type; the kernel itself references no
  * application error type. `Unauthorized` exists for forward use (`AuthEnforcement.Enforce`) but is
  * unreachable under `Skip` — `stripAuth` removes every auth leaf before evaluation.
  */
enum Rejection:
    case Unhandled
    case Invalid(message: UserMessage)
    case Unauthorized(label: String)
end Rejection

object WorkflowInterpreter:

    /** Runs the three-stage interpreter — match, gate, classify → emit — against `defn` under
      * `auth`, for the given `ctx` and `command`.
      *
      *   1. '''Match''': among transitions where `t.from == defn.stateOf(ctx.entity)` and
      *      `t.on.accepts(command)`, find the first whose gated `applicableWhen` passes. No match
      *      and an `applicableWhen` failure both collapse to `Left(Rejection.Unhandled)`.
      *   1. '''Gate''': evaluate the gated `t.requires and t.requiresPayload(entity, command)`. On
      *      failure, collapse the merged `GuardFailure`: a data message ⇒ `Left(Invalid(m))`;
      *      otherwise ⇒ `Left(Unauthorized(label))`.
      *   1. '''Classify → emit''': `Right(t.classify(entity, command).emit(entity, command))`.
      *
      * `gate` applies the auth policy per call: `Skip` rewrites auth leaves away via
      * `Guard.stripAuth`, `Enforce` leaves the guard intact. The interpreter never folds events —
      * it returns the domain `Seq[Event]` as-is; folding into the aggregate stays in the shell.
      */
    def decide[R, State, Entity, Command, Event](
        defn: EngineDefinition[R, State, Entity, Command, Event],
        auth: AuthEnforcement
    )(ctx: GuardContext[R, Entity], command: Command): Either[Rejection, Seq[Event]] =
        val entity = ctx.entity
        val state = defn.stateOf(entity)

        def gate(g: Guard[R, Entity]): Guard[R, Entity] = auth match
            case AuthEnforcement.Skip    => Guard.stripAuth(g)
            case AuthEnforcement.Enforce => g

        val applicable = defn.transitions.iterator
            .filter(t => t.from == state && t.on.accepts(command))
            .find(t => gate(t.applicableWhen).check(ctx).isRight)

        applicable match
            case None => Left(Rejection.Unhandled)
            case Some(t) =>
                val requiresGuard = t.requires and t.requiresPayload(entity, command)
                gate(requiresGuard).check(ctx) match
                    case Left(failure) =>
                        firstDataMessage(failure) match
                            case Some(m) => Left(Rejection.Invalid(m))
                            case None =>
                                Left(Rejection.Unauthorized(firstAuthLabel(failure).getOrElse("")))
                    case Right(()) =>
                        Right(t.classify(entity, command).emit(entity, command))
                end match
        end match
    end decide

    /** DFS-first extraction of a representative value from a merged `GuardFailure` tree. Recurses
      * through `Conjunction`/`Disjunction` in declared order; `extract` picks which leaf kind wins.
      */
    private def collectFirst[A](f: GuardFailure)(extract: PartialFunction[GuardFailure, A])
        : Option[A] =
        f match
            case GuardFailure.Conjunction(fs) =>
                fs.iterator.flatMap(collectFirst(_)(extract)).nextOption()
            case GuardFailure.Disjunction(fs) =>
                fs.iterator.flatMap(collectFirst(_)(extract)).nextOption()
            case other => extract.lift(other)

    private def firstDataMessage(f: GuardFailure): Option[UserMessage] =
        collectFirst(f) { case GuardFailure.Leaf(_, m) => m }

    private def firstAuthLabel(f: GuardFailure): Option[String] =
        collectFirst(f) { case GuardFailure.Unauthorized(label) => label }
end WorkflowInterpreter
