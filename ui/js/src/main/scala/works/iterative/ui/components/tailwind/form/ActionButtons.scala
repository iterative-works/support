package works.iterative
package ui.components.tailwind.form

import com.raquo.laminar.api.L.*
import works.iterative.core.MessageId
import works.iterative.ui.components.tailwind.HtmlComponent
import works.iterative.ui.components.ComponentContext

case class ActionButtonStyle(
    border: String,
    colors: String,
    text: String,
    focus: String,
    extra: String
)

// TODO: enum?
object ActionButtonStyle:
    val default = ActionButtonStyle(
        "border border-gray-300",
        "bg-white text-gray-700 hover:bg-gray-50",
        "text-sm font-medium",
        "focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
        ""
    )
end ActionButtonStyle

case class ActionButton[A](
    name: MessageId,
    action: A,
    style: ActionButtonStyle = ActionButtonStyle.default
):
    def element(actions: Observer[A])(using
        ctx: ComponentContext[?]
    ): HtmlElement =
        button(
            tpe("button"),
            cls("first:ml-0 ml-3"),
            cls("py-2 px-4 rounded-md shadow-sm"),
            cls(style.border),
            cls(style.colors),
            cls(style.text),
            cls(style.extra),
            cls(style.focus),
            ctx.messages(s"action.$name.title"),
            onClick.mapTo(action) --> actions
        )
end ActionButton

// buttons to attach under for or detail cards
case class ActionButtons[A](actions: List[ActionButton[A]])

object ActionButtons:
    class Component[A](actions: Observer[A])(using ctx: ComponentContext[?])
        extends HtmlComponent[org.scalajs.dom.html.Div, ActionButtons[A]]:
        override def render(v: ActionButtons[A]) =
            div(
                cls("flex justify-end"),
                v.actions.map(_.element(actions))
            )
    end Component
end ActionButtons
