package portaly.forms

import zio.NonEmptyChunk
import works.iterative.core.UserMessage
import com.raquo.airstream.core.EventStream
import works.iterative.core.MessageCatalogue
import zio.prelude.*
import works.iterative.ui.model.forms.IdPath

sealed trait ValidationState[+OutputValue]:
    self =>
    def isValid: Boolean
    def isInvalid: Boolean = !isValid
    def map[B](f: OutputValue => B): ValidationState[B] =
        flatMap(v => ValidationState.Valid(f(v)))

    def flatMap[B](f: OutputValue => ValidationState[B]): ValidationState[B] =
        self match
            case ValidationState.Valid(o)      => f(o)
            case ValidationState.Invalid(err)  => ValidationState.Invalid(err)
            case ValidationState.Unknown(info) => ValidationState.Unknown(info)

    def toOption: Option[OutputValue] =
        self match
            case ValidationState.Valid(o) => Some(o)
            case _                        => None
end ValidationState

object ValidationState:
    final case class Valid[OutputValue](value: OutputValue) extends ValidationState[OutputValue]:
        override val isValid: Boolean = true

    final case class Invalid(errors: NonEmptyChunk[(IdPath, UserMessage)])
        extends ValidationState[Nothing]:
        override val isValid: Boolean = false

    object Invalid:
        def apply(id: IdPath, msg: UserMessage): Invalid = Invalid(NonEmptyChunk((id, msg)))

    final case class Unknown(info: List[(IdPath, UserMessage)]) extends ValidationState[Nothing]:
        override val isValid: Boolean = false

    def failIf[A](id: IdPath)(condition: A => Boolean)(failMsg: UserMessage)(a: A)
        : ValidationState[A] =
        if condition(a) then ValidationState.Invalid(id, failMsg) else ValidationState.Valid(a)

    def failUnless[A](id: IdPath)(condition: A => Boolean)(failMsg: UserMessage)(a: A)
        : ValidationState[A] =
        failIf[A](id)(a => !condition(a))(failMsg)(a)

    def lift[A, B](f: A => ValidationState[B]): ValidationState[A] => ValidationState[B] =
        _.flatMap(f)

    def liftM[F[+_]: IdentityBoth: Covariant, A, B](f: A => F[ValidationState[B]])
        : ValidationState[A] => F[ValidationState[B]] = {
        case ValidationState.Valid(o)      => f(o)
        case ValidationState.Unknown(info) => ValidationState.Unknown(info).succeed
        case ValidationState.Invalid(err)  => ValidationState.Invalid(err).succeed
    }

    @deprecated
    def required[OutputValue](id: IdPath, inp: String, required: Boolean)(using
        MessageCatalogue
    )(
        otherValidations: => EventStream[ValidationState[OutputValue]]
    ): EventStream[ValidationState[OutputValue]] =
        if inp.isBlank && required then
            EventStream.fromValue(
                ValidationState.Invalid(
                    id,
                    UserMessage("error.field.required", id.toMessage("label"))
                )
            )
        else otherValidations

    given identityValidationState[A: Identity]: Identity[ValidationState[A]] with
        override def combine(
            x: => ValidationState[A],
            y: => ValidationState[A]
        ): ValidationState[A] =
            (x, y) match
                case (Unknown(infos), Invalid(errs))       => y
                case (Unknown(infos), Unknown(otherInfos)) => Unknown(infos ++ otherInfos)
                case (Unknown(infos), _)                   => x
                case (Invalid(errs), Invalid(otherErrs))   => Invalid(errs ++ otherErrs)
                case (Invalid(errs), _)                    => x
                case (Valid(a), Valid(b))                  => Valid(a.combine(b))
                case _                                     => y
            end match
        end combine

        override def identity: ValidationState[A] = ValidationState.Valid(Identity[A].identity)
    end identityValidationState
end ValidationState
