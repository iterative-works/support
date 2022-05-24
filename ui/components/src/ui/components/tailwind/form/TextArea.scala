package works.iterative
package ui.components.tailwind.form

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

class TextArea[V](
    initialRows: Int = 5
)(using codec: FormCodec[V, String])
    extends FormInput[V]:
  override def render(
      prop: Property[V],
      updates: Observer[Validated[V]]
  ): ReactiveHtmlElement[html.TextArea] =
    def numberOfLines(s: String) = s.count(_ == '\n')
    val currentValue = prop.value.map(codec.toForm)
    val changeBus = EventBus[String]()
    val rowNo = Var(currentValue.map(numberOfLines).getOrElse(initialRows))
    textArea(
      changeBus.events.map(numberOfLines) --> rowNo,
      changeBus.events.map(codec.toValue) --> updates,
      name := prop.name,
      rows <-- rowNo.signal.map(_ + 2),
      cls := "max-w-lg shadow-sm block w-full focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border border-gray-300 rounded-md",
      prop.value.map(v => value(codec.toForm(v))),
      onInput.mapToValue.setAsValue --> changeBus.writer
    )
