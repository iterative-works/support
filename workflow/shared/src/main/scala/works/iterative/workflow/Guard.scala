// PURPOSE: Defines Guard — composable guard carrying both describe and check in one object
// PURPOSE: Implements sum-aware failure algebra with flat Conjunction/Disjunction accumulation
package works.iterative.workflow

import works.iterative.core.UserMessage

/** A composable guard over entity state S.
  *
  * Each guard carries:
  *   - `describe` — a renderable `GuardDescription` tree (consumed by diagram export)
  *   - `check` — a pure evaluation against a `GuardContext`
  *
  * The `describe` tree mirrors the evaluation structure:
  *   - `And` → `All` (flattened, matching `And.check`'s flat Conjunction accumulation)
  *   - `Or` → `Any` (node-level, matching `Or.check`'s node-level Disjunction)
  *
  * Structural mirroring guarantees the description cannot contradict evaluation.
  *
  * Combinators:
  *   - `and`: accumulates all failing branches flatly into `GuardFailure.Conjunction`
  *   - `or`: node-level; if both branches fail, wraps them in `GuardFailure.Disjunction`
  */
sealed trait Guard[R, S]:
    def describe: GuardDescription
    def check(ctx: GuardContext[R, S]): Either[GuardFailure, Unit]
    infix def and(that: Guard[R, S]): Guard[R, S] = Guard.And(this, that)
    infix def or(that: Guard[R, S]): Guard[R, S] = Guard.Or(this, that)
end Guard

object Guard:
    /** Data guard leaf — evaluates `predicate` against the entity; failure → `GuardFailure.Leaf`.
      */
    final case class Requirement[R, S](predicate: Predicate[S], message: UserMessage)
        extends Guard[R, S]:
        def describe: GuardDescription = GuardDescription.Leaf(predicate.label)
        def check(ctx: GuardContext[R, S]): Either[GuardFailure, Unit] =
            if predicate(ctx.entity) then Right(())
            else Left(GuardFailure.Leaf(predicate.label, message))
    end Requirement

    /** Always-failing data guard leaf — for guard branches whose failure condition is a property of
      * the command payload, not the entity, so no genuine `Predicate[S]` test applies (e.g. a
      * payload-dependent `requiresPayload` branch). Mirrors `Requirement`'s describe/check shape
      * without a predicate: `check` always returns `Left(GuardFailure.Leaf(label, message))`.
      */
    final case class Invalid[R, S](label: String, message: UserMessage) extends Guard[R, S]:
        def describe: GuardDescription = GuardDescription.Leaf(label)
        def check(ctx: GuardContext[R, S]): Either[GuardFailure, Unit] =
            Left(GuardFailure.Leaf(label, message))
    end Invalid

    /** Convenience constructor for `Invalid` — an always-failing guard leaf. */
    def invalid[R, S](label: String, message: UserMessage): Guard[R, S] = Invalid(label, message)

    /** Auth guard leaf — checks that the caller holds `role`; failure →
      * `GuardFailure.Unauthorized`.
      */
    final case class RequireRole[R, S](role: R) extends Guard[R, S]:
        def describe: GuardDescription = GuardDescription.Leaf(role.toString)
        def check(ctx: GuardContext[R, S]): Either[GuardFailure, Unit] =
            if ctx.roles.contains(role) then Right(())
            else Left(GuardFailure.Unauthorized(role.toString))
    end RequireRole

    /** Auth guard leaf — evaluates an arbitrary identity closure; failure →
      * `GuardFailure.Unauthorized`.
      *
      * The `holds` closure receives the full `GuardContext`, allowing it to bridge `userId` (the
      * caller's identity) against entity-held handles (e.g. the assigned `VedouciProjektu`). The
      * bridging logic lives inside the closure, not in the kernel.
      */
    final case class RequireIdentity[R, S](label: String, holds: GuardContext[R, S] => Boolean)
        extends Guard[R, S]:
        def describe: GuardDescription = GuardDescription.Leaf(label)
        def check(ctx: GuardContext[R, S]): Either[GuardFailure, Unit] =
            if holds(ctx) then Right(())
            else Left(GuardFailure.Unauthorized(label))
    end RequireIdentity

    /** Conjunction guard — both branches must pass.
      *
      * Failures are accumulated flatly: any inner `Conjunction` is flattened into the list, so
      * `Conjunction(Conjunction(A, B), C)` evaluates to `Conjunction(List(A, B, C))`. This
      * guarantees no redundant nesting of `Conjunction` nodes.
      */
    final case class And[R, S](left: Guard[R, S], right: Guard[R, S]) extends Guard[R, S]:
        def describe: GuardDescription =
            val lParts = left.describe match
                case GuardDescription.All(ps) => ps
                case d                        => List(d)
            val rParts = right.describe match
                case GuardDescription.All(ps) => ps
                case d                        => List(d)
            GuardDescription.All(lParts ++ rParts)
        end describe

        def check(ctx: GuardContext[R, S]): Either[GuardFailure, Unit] =
            (left.check(ctx), right.check(ctx)) match
                case (Right(()), Right(())) => Right(())
                case (Left(lf), Right(()))  => Left(lf)
                case (Right(()), Left(rf))  => Left(rf)
                case (Left(lf), Left(rf)) =>
                    val lParts = lf match
                        case GuardFailure.Conjunction(fs) => fs
                        case _                            => List(lf)
                    val rParts = rf match
                        case GuardFailure.Conjunction(fs) => fs
                        case _                            => List(rf)
                    Left(GuardFailure.Conjunction(lParts ++ rParts))
    end And

    /** Disjunction guard — at least one branch must pass.
      *
      * Node-level: if both branches fail, their failures are wrapped in `GuardFailure.Disjunction`
      * with exactly two elements (no flattening of inner `Disjunction` nodes).
      */
    final case class Or[R, S](left: Guard[R, S], right: Guard[R, S]) extends Guard[R, S]:
        def describe: GuardDescription =
            GuardDescription.Any(List(left.describe, right.describe))

        def check(ctx: GuardContext[R, S]): Either[GuardFailure, Unit] =
            left.check(ctx) match
                case Right(()) => Right(())
                case Left(lf) =>
                    right.check(ctx) match
                        case Right(()) => Right(())
                        case Left(rf)  => Left(GuardFailure.Disjunction(List(lf, rf)))
    end Or

    private final class Always[R, S] extends Guard[R, S]:
        def describe: GuardDescription = GuardDescription.All(Nil)
        def check(ctx: GuardContext[R, S]): Either[GuardFailure, Unit] = Right(())
    end Always

    /** A guard that always passes — default for transitions with no applicable condition. */
    def always[R, S]: Guard[R, S] = Always()

    /** Rewrites `RequireRole`/`RequireIdentity` leaves to `always`, recursively through `And`/`Or`.
      *
      * Used by `WorkflowInterpreter.decide` under `AuthEnforcement.Skip` — a tree-rewrite, not
      * collapse-time failure-stripping, so `Or(RequireRole, Requirement)` under `Skip` PASSES even
      * when the `Requirement` fails (only leaf-rewriting yields that).
      */
    def stripAuth[R, S](g: Guard[R, S]): Guard[R, S] = g match
        case _: RequireRole[?, ?] | _: RequireIdentity[?, ?] => Guard.always
        case And(l, r)                                       => And(stripAuth(l), stripAuth(r))
        case Or(l, r)                                        => Or(stripAuth(l), stripAuth(r))
        case other                                           => other
end Guard
