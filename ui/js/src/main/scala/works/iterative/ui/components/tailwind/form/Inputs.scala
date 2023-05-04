package works.iterative
package ui.components.tailwind.form

import com.raquo.laminar.api.L.{*, given}
import scala.scalajs.js
import com.raquo.laminar.nodes.ReactiveHtmlElement
import java.time.LocalDate

object Inputs:

  private def inp[V](
      prop: Property[V],
      updates: Observer[Validated[V]],
      inputType: String,
      mods: Option[Modifier[Input]] = None
  )(using codec: FormCodec[V, String]): Input =
    input(
      idAttr := prop.id,
      nameAttr := prop.name,
      tpe := inputType,
      cls := "block max-w-lg w-full shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border-gray-300 rounded-md",
      prop.value.map(v => value(codec.toForm(v))),
      onInput.mapToValue.setAsValue.map(v => codec.toValue(v)) --> updates
    )

  class PlainInput[V](using FormCodec[V, String]) extends FormInput[V]:
    override def render(
        prop: Property[V],
        updates: Observer[Validated[V]]
    ): Input = inp(prop, updates, "text")

  class OptionDateInput extends FormInput[Option[LocalDate]]:
    override def render(
        prop: Property[Option[LocalDate]],
        updates: Observer[Validated[Option[LocalDate]]]
    ): Input =
      inp(prop, updates, "date", Some(autoComplete("date")))
