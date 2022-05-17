package works.iterative
package ui.components.tailwind.form

import core.PlainMultiLine

import com.raquo.laminar.api.L.{*, given}
import java.time.LocalDate

trait FormInput[V]:
  def render(
      property: Property[V],
      updates: Observer[Validated[V]]
  ): HtmlElement

object FormInput:
  given plainMultiLineInput: FormInput[PlainMultiLine] = TextArea()
  given optionPlainMultiLineInput: FormInput[Option[PlainMultiLine]] =
    TextArea()
  given optionLocalDateInput: FormInput[Option[LocalDate]] =
    Inputs.OptionDateInput()
