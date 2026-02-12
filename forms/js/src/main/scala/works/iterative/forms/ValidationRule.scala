package portaly.forms

import zio.prelude.*
import works.iterative.core.UserMessage
import works.iterative.core.ValidatedStringFactory
import works.iterative.ui.model.forms.IdPath

trait ValidationRule[F[+_], -RawValue, +OutputValue]
    extends (RawValue => F[ValidationState[OutputValue]]):
    self =>
    def map[B](f: OutputValue => B)(using Covariant[F]): ValidationRule[F, RawValue, B] =
        ValidationRule.Mapped(self, f)
    def contramap[B](f: B => RawValue): ValidationRule[F, B, OutputValue] =
        ValidationRule.Contramapped(self, f)
    def flatMap[O1](other: ValidationRule[F, OutputValue, O1])(using
        IdentityBoth[F],
        AssociativeFlatten[F],
        Covariant[F]
    ): ValidationRule[F, RawValue, O1] =
        ValidationRule.FlatMapped(self, other)
end ValidationRule

// Symetric validation rule, same input as output
type SValidationRule[F[+_], O] = ValidationRule[F, O, O]

object ValidationRule:
    final case class Mapped[F[+_]: Covariant, R, O, O1](rule: ValidationRule[F, R, O], f: O => O1)
        extends ValidationRule[F, R, O1]:
        def apply(v: R): F[ValidationState[O1]] =
            Covariant[F].map((_: ValidationState[O]).map(f))(rule(v))

    final case class Contramapped[F[+_], R, R1, O](rule: ValidationRule[F, R, O], f: R1 => R)
        extends ValidationRule[F, R1, O]:
        def apply(v: R1): F[ValidationState[O]] = rule(f(v))

    final case class FlatMapped[F[+_]: IdentityBoth: AssociativeFlatten: Covariant, R, O, O1](
        rule: ValidationRule[F, R, O],
        nextRule: ValidationRule[F, O, O1]
    ) extends ValidationRule[F, R, O1]:
        def apply(v: R): F[ValidationState[O1]] =
            val next: ValidationRule[F, ValidationState[O], O1] =
                ValidationRule(ValidationState.liftM(nextRule))
            // Ugly. The givens are not found automatically.
            AssociativeFlattenOps[F, ValidationState[O1]](
                CovariantOps[F, ValidationState[O]](rule(v)).map[F[ValidationState[O1]]](next)
            ).flatten
        end apply
    end FlatMapped

    def apply[F[+_], R, O](rule: R => F[ValidationState[O]]): ValidationRule[F, R, O] =
        new ValidationRule[F, R, O]:
            def apply(r: R): F[ValidationState[O]] = rule(r)

    def valid[F[+_]: IdentityBoth: Covariant, A]: ValidationRule[F, A, A] =
        (v: A) => ValidationState.Valid(v).succeed

    def succeed[F[+_]: IdentityBoth: Covariant, A, B](f: A => ValidationState[B])
        : ValidationRule[F, A, B] =
        f(_).succeed

    def fromZValidation[F[+_]: IdentityBoth: Covariant, A](id: IdPath)(v: A => ZValidation[
        ?,
        UserMessage,
        A
    ]): SValidationRule[F, A] =
        a =>
            v(a).fold(
                errs => ValidationState.Invalid(errs.map(id -> _)),
                ValidationState.Valid(_)
            ).succeed

    def fromValidatedString[F[+_]: IdentityBoth: Covariant, A](id: IdPath)(
        v: ValidatedStringFactory[A]
    ): SValidationRule[F, String] =
        fromZValidation(id)(v(_).map(v.value(_)))

    def nonEmpty[F[+_]: IdentityBoth: Covariant, G[+_]: ForEach, A](id: IdPath)(msg: UserMessage)
        : SValidationRule[F, G[A]] =
        (v: G[A]) =>
            if ForEach[G].isEmpty(v) then ValidationState.Invalid(id, msg).succeed
            else ValidationState.Valid(v).succeed

    def required[F[+_]: IdentityBoth: Covariant, G[+_]: ForEach, A: Identity](id: IdPath)(
        msg: UserMessage
    ): ValidationRule[F, G[A], A] =
        (v: G[A]) =>
            if ForEach[G].isEmpty(v) then ValidationState.Invalid(id, msg).succeed
            else ValidationState.Valid(ForEach[G].reduceIdentity(v)).succeed

    /*
    given covariantValidationRule[F[_]: Covariant, R, A]: Covariant[[A] =>> ValidationRule[F, R, A]] with
        override def map[B](f: A => B): ValidationRule[F, R, A] => ValidationRule[F, R, B] = rule =>
            ValidationRule(v => CovariantOps[F, ValidationState[A]](rule(v)).map(_.map(f)))
    end covariantValidationRule

    given contravariantValidationRule[F[_], R, A]: Contravariant[ValidationRule[F, *, A]] with
        override def contramap[R1](f: R1 => R)
            : ValidationRule[F, R, A] => ValidationRule[F, R1, A] = rule =>
            ValidationRule((v: R1) => rule(f(v)))
    end contravariantValidationRule
     */
end ValidationRule
