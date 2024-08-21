package portaly.forms

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import zio.prelude.*
import works.iterative.ui.model.forms.{RelativePath, AbsolutePath, IdPath}

enum FormControl:
    case Disable(path: IdPath)
    case Enable(path: IdPath)
    case Invoke(path: IdPath)
end FormControl

trait FormPartInputs[+S, +RI, +EI]:
    import FormPartInputs.*

    def id: AbsolutePath
    def formState: S
    def showErrors: Signal[Boolean]
    def rawInput: EventStream[RI]
    def errorInput: EventStream[EI]
    def control: EventStream[FormControl]

    def mapRawInput[RI1](f: RI => RI1): FormPartInputs[S, RI1, EI] =
        MappedRawInput(this, f)

    def composeRawInput[RI1 >: RI](f: EventStream[RI] => EventStream[RI1])
        : FormPartInputs[S, RI1, EI] =
        ComposedRawInput(this, f)

    def mapError[E1](f: EI => E1): FormPartInputs[S, RI, E1] =
        MappedError(this, f)

    def withId(id: AbsolutePath): FormPartInputs[S, RI, EI] =
        MappedId(this, _ => id)

    def mapId(f: AbsolutePath => AbsolutePath): FormPartInputs[S, RI, EI] =
        MappedId(this, f)
end FormPartInputs

object FormPartInputs:
    def apply[S, RI, EI](
        i: AbsolutePath,
        s: S,
        show: Signal[Boolean],
        r: EventStream[RI],
        e: EventStream[EI],
        c: EventStream[FormControl]
    ): FormPartInputs[S, RI, EI] =
        new FormPartInputs[S, RI, EI]:
            override val id: AbsolutePath = i
            override val formState: S = s
            override val showErrors: Signal[Boolean] = show
            override val rawInput: EventStream[RI] = r
            override val errorInput: EventStream[EI] = e
            override val control: EventStream[FormControl] = c

    private case class MappedError[S, RI, EI, E1](o: FormPartInputs[S, RI, EI], f: EI => E1)
        extends FormPartInputs[S, RI, E1]:
        export o.{id, formState, showErrors, rawInput, control}
        override val errorInput: EventStream[E1] = o.errorInput.map(f)
    end MappedError

    private case class MappedId[S, RI, EI](
        o: FormPartInputs[S, RI, EI],
        f: AbsolutePath => AbsolutePath
    ) extends FormPartInputs[S, RI, EI]:
        export o.{formState, showErrors, rawInput, errorInput, control}
        override val id: AbsolutePath = f(o.id)
    end MappedId

    private case class MappedRawInput[S, RI, EI, RI1](
        o: FormPartInputs[S, RI, EI],
        f: RI => RI1
    ) extends FormPartInputs[S, RI1, EI]:
        export o.{id, formState, showErrors, errorInput, control}
        override val rawInput: EventStream[RI1] = o.rawInput.map(f)
    end MappedRawInput

    private case class ComposedRawInput[S, RI, RI1, EI](
        o: FormPartInputs[S, RI, EI],
        f: EventStream[RI] => EventStream[RI1]
    ) extends FormPartInputs[S, RI1, EI]:
        export o.{id, formState, showErrors, errorInput, control}
        override val rawInput: EventStream[RI1] = f(o.rawInput)
    end ComposedRawInput
end FormPartInputs

trait FormPartOutputs[+RO, +EO, +A]:
    import FormPartOutputs.*

    def id: AbsolutePath
    def errorOutput: EventStream[EO]
    def rawOutput: Signal[RO]
    def output: Signal[A]
    def domOutput: HtmlElement

    def map[A1](f: A => A1): FormPartOutputs[RO, EO, A1] =
        Mapped(this, f)

    def mapError[E1](f: EO => E1): FormPartOutputs[RO, E1, A] =
        MappedError(this, f)

    def mapDom(f: HtmlElement => HtmlElement): FormPartOutputs[RO, EO, A] =
        MappedDom(this, f)

    def tap(f: Observer[(AbsolutePath, A)]): FormPartOutputs[RO, EO, A] =
        mapDom(_.amend(output.changes.map(id -> _) --> f))
end FormPartOutputs

object FormPartOutputs:
    private val unitSignal: Signal[Unit] = Signal.fromValue(())
    private val noneSignal: Signal[Option[Nothing]] = Signal.fromValue(None)

    def apply[RO, EO, A](
        i: AbsolutePath,
        r: Signal[RO],
        e: EventStream[EO],
        o: Signal[A],
        d: HtmlElement
    ): FormPartOutputs[RO, EO, A] =
        new FormPartOutputs[RO, EO, A]:
            override val id: AbsolutePath = i
            override val rawOutput: Signal[RO] = r
            override val errorOutput: EventStream[EO] = e
            override val output: Signal[A] = o
            override val domOutput: HtmlElement = d

    def element(id: AbsolutePath, d: HtmlElement): FormPartOutputs[Unit, Nothing, Unit] =
        FormPartOutputs(id, unitSignal, EventStream.empty, unitSignal, d)

    def succeed[R](id: AbsolutePath, o: Signal[R], d: HtmlElement): FormPartOutputs[R, Nothing, R] =
        FormPartOutputs(id, o, EventStream.empty, o, d)

    def emptyOutputs(id: AbsolutePath): FormPartOutputs[Unit, Unit, Unit] =
        FormPartOutputs.element(id, div())

    def when[RO, EO, A](condition: Signal[Boolean])(outputs: FormPartOutputs[RO, EO, A])
        : FormPartOutputs[RO, EO, Option[A]] =
        FormPartOutputs(
            outputs.id,
            outputs.rawOutput,
            outputs.errorOutput,
            condition.flatMapSwitch(c =>
                if c then outputs.output.map(Some(_)) else noneSignal
            ),
            div(
                condition.childWhenTrue(outputs.domOutput)
            )
        )

    private case class Mapped[RO, EO, A, A1](o: FormPartOutputs[RO, EO, A], f: A => A1)
        extends FormPartOutputs[RO, EO, A1]:
        export o.{id, rawOutput, errorOutput, domOutput}
        override val output: Signal[A1] = o.output.map(f)
    end Mapped

    private case class MappedError[RO, EO, E1, A](o: FormPartOutputs[RO, EO, A], f: EO => E1)
        extends FormPartOutputs[RO, E1, A]:
        export o.{id, rawOutput, output, domOutput}
        override val errorOutput: EventStream[E1] = o.errorOutput.map(f)
    end MappedError

    private case class MappedDom[RO, EO, A](
        o: FormPartOutputs[RO, EO, A],
        f: HtmlElement => HtmlElement
    ) extends FormPartOutputs[RO, EO, A]:
        export o.{id, rawOutput, output, errorOutput}
        override val domOutput: HtmlElement = f(o.domOutput)
    end MappedDom
end FormPartOutputs

/** @tparam GR
  *   Global raw value of the whole form
  * @tparam RI
  *   Raw input for this part of form
  * @tparam RO
  *   Raw output for this part of form
  * @tparam EI
  *   Error input for this part of form
  * @tparam EO
  *   Error output for this part of form
  * @tparam A
  *   Output result for this form part
  */
type FormPart[-S, -RI, -EI, +RO, +EO, +A] = FormPartInputs[S, RI, EI] => FormPartOutputs[RO, EO, A]

object FormPart:
    type StringInput = FormPart[Any, String, Boolean, String, Nothing, String]

    def fromVar[A](
        id: IdPath,
        value: Var[A],
        validation: A => ValidationState[A] = (a: A) => ValidationState.Valid(a)
    )(render: Var[A] => FormPartInputs[Any, A, Nothing] => HtmlElement)
        : FormPart[Any, A, Nothing, A, Nothing, ValidationState[A]] =
        fi =>
            FormPartOutputs(
                fi.id,
                value.signal,
                EventStream.empty,
                value.signal.map(validation).map {
                    case ValidationState.Invalid(errs) =>
                        ValidationState.Invalid(errs.map((_, msg) => fi.id -> msg))
                    case v => v
                },
                render(value)(fi).amend(
                    fi.rawInput --> value.writer
                )
            )

    def domOnly(
        elem: FormPartInputs[Any, Unit, Nothing] => HtmlElement
    ): FormPart[Any, Unit, Nothing, Unit, Nothing, Unit] =
        fi => FormPartOutputs.element(fi.id, elem(fi))

    def empty: FormPart[Any, Unit, Nothing, Unit, Nothing, Unit] =
        domOnly(_ => div())

    def combine[S, RI, EI, RO: Identity, EO: Identity, A: Identity](
        id: RelativePath,
        parts: FormPart[S, RI, EI, RO, EO, A]*
    )(
        render: Seq[HtmlElement] => HtmlElement
    ): FormPart[S, RI, EI, RO, EO, A] = fi =>
        val outputs = parts.map(p => p(fi.mapId(_ / id)))
        FormPartOutputs(
            fi.id,
            Signal.combineSeq(outputs.map(_.rawOutput)).map(_.reduceIdentity),
            EventStream.combineSeq(outputs.map(_.errorOutput)).map(_.reduceIdentity),
            Signal.combineSeq(outputs.map(_.output)).map(_.reduceIdentity),
            render(outputs.map(_.domOutput))
        )
end FormPart

extension [S, RI, EI, RO, EO, A](part: FormPart[S, RI, EI, RO, EO, A])
    def mapDom(render: HtmlElement => HtmlElement): FormPart[S, RI, EI, RO, EO, A] =
        fi => part(fi).mapDom(render)
    def map[A1](f: A => A1): FormPart[S, RI, EI, RO, EO, A1] =
        fi => part(fi).map(f)
    def valid: FormPart[S, RI, EI, RO, EO, ValidationState.Valid[A]] =
        fi => part(fi).map(ValidationState.Valid(_))
    def tap(f: Observer[(AbsolutePath, A)]): FormPart[S, RI, EI, RO, EO, A] =
        fi => part(fi).tap(f)
    def withId(id: AbsolutePath): FormPart[S, RI, EI, RO, EO, A] =
        fi => part(fi.withId(id))
    def mapId(f: AbsolutePath => AbsolutePath): FormPart[S, RI, EI, RO, EO, A] =
        fi => part(fi.mapId(f))
    def mapError[E1](f: EO => E1): FormPart[S, RI, EI, RO, E1, A] =
        fi => part(fi).mapError(f)
end extension
