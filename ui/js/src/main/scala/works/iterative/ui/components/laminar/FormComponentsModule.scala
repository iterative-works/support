package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.raquo.laminar.keys.ReactiveProp
import com.raquo.domtypes.jsdom.defs.events.TypedTargetEvent
import org.scalajs.dom.html
import com.raquo.laminar.modifiers.KeyUpdater

/** Form components
  *
  * A form is a collection of sections, each of which contains a collection of
  * fields.
  *
  * Form -> * Section -> * Field
  * -> * Action
  *
  * Each Field has an `id`, `label` and `input` element. The `id` is used to
  * link the label to the input.
  *
  * The `input` element can be a simple text input, or a more complex component
  * such as a date picker. The caller is expected to make the input work.
  */
trait FormComponentsModule extends LocalDateSelectModule:
  def forms: FormComponents

  trait FormComponents:

    /** Layout the sections and actions of a form */
    def form(mods: Modifier[HtmlElement]*)(sections: Modifier[HtmlElement]*)(
        actions: Modifier[HtmlElement]*
    ): HtmlElement

    /** Section layout, with a title, optional subtitle and any content. */
    def section(
        title: Modifier[HtmlElement],
        subtitle: Option[Modifier[HtmlElement]]
    )(
        content: Modifier[HtmlElement]*
    ): HtmlElement

    /** Layout a field with label and any content */
    def field(label: Modifier[HtmlElement])(
        content: Modifier[HtmlElement]*
    ): HtmlElement

    /** Layout a field with label, input and optional help text */
    def field(
        id: String,
        label: String,
        input: HtmlElement,
        help: Option[String]
    ): HtmlElement

    def label(labelText: String, forId: Option[String] = None)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement

    /** Layout an inline form element */
    def inlineForm(mods: Modifier[HtmlElement]*): HtmlElement

    /** An input field with a search icon */
    def searchField(id: String, placeholderText: Option[String] = None)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement

    def inputField(
        id: String,
        labelText: String,
        placeholderText: Option[String] = None,
        inputType: String = "text",
        helpText: Option[String] = None
    ): HtmlElement

    /** LocalDate input */
    def renderLocalDateSelect(
        id: String,
        labelText: Option[String],
        placeholderText: Option[String],
        mods: LocalDateSelect => Modifier[HtmlElement]
    ): HtmlElement
