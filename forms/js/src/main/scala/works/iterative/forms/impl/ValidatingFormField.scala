package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import works.iterative.core.UserMessage
import com.raquo.airstream.core.EventStream
import zio.prelude.*

given IdentityBoth[EventStream] with
    override def any: EventStream[Any] = EventStream.unit()
    override def both[A, B](fa: => EventStream[A], fb: => EventStream[B]): EventStream[(A, B)] =
        fa.zip(fb)
end given

given Covariant[EventStream] with
    override def map[A, B](f: A => B): EventStream[A] => EventStream[B] = _.map(f)

given AssociativeFlatten[EventStream] with
    override def flatten[A](ffa: EventStream[EventStream[A]]): EventStream[A] =
        ffa.flattenMerge

type VRule[-R, +O] = ValidationRule[EventStream, R, O]
type SRule[O] = VRule[O, O]

class ValidatingFormField[-S, -RI, +RO, +O, +A](validationRule: VRule[O, A])(
    field: FormPart[S, RI, List[UserMessage], RO, Nothing, O],
    touched: Signal[Boolean]
) extends FormPart[S, RI, Nothing, RO, List[UserMessage], ValidationState[A]]:
    private val validationResult: Var[ValidationState[A]] = Var(ValidationState.Unknown(Nil))

    private def domOutput(
        innerOutputs: FormPartOutputs[RO, Nothing, O],
        errors: Observer[List[UserMessage]]
    ): HtmlElement =
        validationResult.set(ValidationState.Unknown(List(innerOutputs.id -> UserMessage(
            "error.validation.unknown",
            innerOutputs.id.toString()
        ))))
        innerOutputs.domOutput.amend(
            innerOutputs.output.changes.throttle(500, false)
                .setDisplayName("to_validate")
                .flatMapSwitch(validationRule) --> validationResult.writer.setDisplayName(
                "validation"
            ),
            validationResult.signal.changes.collect {
                case ValidationState.Invalid(errs) => errs.toList.map(_._2)
                case _                             => Nil
            } --> errors,
            // Init the validation state
            EventStream.unit().sample(innerOutputs.output).flatMapSwitch(
                validationRule
            ) --> validationResult.writer
        )
    end domOutput

    override def apply(fi: FormPartInputs[S, RI, Nothing])
        : FormPartOutputs[RO, List[UserMessage], ValidationState[A]] =
        val (errors, errorsObserver) = EventStream.withObserver[List[UserMessage]]

        val innerInputs: FormPartInputs[S, RI, List[UserMessage]] =
            FormPartInputs(
                fi.id,
                fi.formState,
                (fi.showErrors || touched).setDisplayName(
                    s"show_errors_vf:${fi.id.toHtmlId}"
                ),
                fi.rawInput,
                errors,
                fi.control
            )

        val innerOutputs = field(innerInputs)

        FormPartOutputs(
            innerOutputs.id,
            innerOutputs.rawOutput.setDisplayName(s"validated_raw:${fi.id.toHtmlId}"),
            errors,
            validationResult.signal.setDisplayName(s"validated_out:${fi.id.toHtmlId}"),
            domOutput(innerOutputs, errorsObserver)
        )
    end apply
end ValidatingFormField
