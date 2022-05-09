package works.iterative
package ui.components.tailwind.form

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.UIString
import works.iterative.ui.components.tailwind.HtmlComponent

case class ActionButton[A](
    title: UIString,
    action: A
)

// buttons to attach under for or detail cards
case class ActionButtons[A](
    actions: List[ActionButton[A]]
)

object ActionButtons:
  class Component[A](actions: Observer[A])
      extends HtmlComponent[org.scalajs.dom.html.Div, ActionButtons[A]]:
    override def render(v: ActionButtons[A]) =
      div(
        cls("flex justify-end"),
        v.actions.zipWithIndex.map { case (ActionButton(title, action), idx) =>
          button(
            tpe("button"),
            cls(if idx == 0 then "" else "ml-3"),
            cls(
              "bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            ),
            title,
            onClick.mapTo(action) --> actions
          )
        }
      )
