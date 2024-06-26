package works.iterative
package ui.components.tailwind.form

import core.PlainMultiLine

import com.raquo.laminar.api.L.*
import java.time.LocalDate
import works.iterative.ui.components.ComponentContext

trait FormInput[V]:
    def render(
        property: Property[V],
        updates: Observer[Validated[V]]
    ): HtmlElement

    def validate(v: V => Validated[V]): FormInput[V] =
        (property: Property[V], updates: Observer[Validated[V]]) =>
            this.render(property, updates.contramap(_.flatMap(v)))
end FormInput

object FormInput:
    given stringInput: FormInput[String] = Inputs.PlainInput()
    given optionStringInput: FormInput[Option[String]] = Inputs.PlainInput()
    given plainMultiLineInput: FormInput[PlainMultiLine] = TextArea()
    given optionPlainMultiLineInput: FormInput[Option[PlainMultiLine]] =
        TextArea()
    given optionLocalDateInput: FormInput[Option[LocalDate]] =
        Inputs.OptionDateInput()
    given optionBooleanInput(using
        ComponentContext[?]
    ): FormInput[Option[Boolean]] =
        Switch()
end FormInput
