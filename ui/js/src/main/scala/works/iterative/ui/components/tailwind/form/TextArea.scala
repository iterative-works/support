package works.iterative
package ui.components.tailwind.form

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

class TextArea[V](using codec: FormCodec[V, String]) extends FormInput[V]:
  override def render(
      prop: Property[V],
      updates: Observer[Validated[V]]
  ): ReactiveHtmlElement[html.TextArea] =
    TextArea.render(
      prop.name,
      prop.value.map(codec.toForm),
      updates.contramap(codec.toValue),
      cls(
        "max-w-lg w-full shadow-sm block focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border border-gray-300 rounded-md"
      )
    )

object TextArea:
  def render(
      fieldName: String,
      currentValue: Option[String],
      updates: Observer[String],
      mods: Modifier[ReactiveHtmlElement[html.TextArea]]*
  ): ReactiveHtmlElement[html.TextArea] =
    def numberOfLines(s: String) = s.count(_ == '\n')
    val changeBus = EventBus[String]()
    val rowNo = Var(currentValue.map(numberOfLines).getOrElse(0))
    textArea(
      changeBus.events.map(numberOfLines) --> rowNo,
      changeBus.events --> updates,
      nameAttr := fieldName,
      rows <-- rowNo.signal.map(_ + 2),
      mods,
      currentValue.map(value(_)),
      onInput.mapToValue.setAsValue --> changeBus.writer
    )
