package mdr.pdb.app.pages.directory.components

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.Icons

object SearchForm:
  sealed trait Action
  case object SubmitFilter extends Action
  sealed trait FilterAction extends Action
  case object ClearFilter extends FilterAction
  case class SetFilter(value: String) extends FilterAction

  def apply(actions: Observer[Action]): HtmlElement =
    div(
      cls := "px-6 pt-4 pb-4",
      form(
        cls := "flex space-x-4",
        onSubmit.mapTo(SubmitFilter) --> actions,
        div(
          cls := "flex-1 min-w-0",
          label(
            forId := "search",
            cls := "sr-only",
            "Hledat"
          ),
          div(
            cls := "relative rounded-md shadow-sm",
            div(
              cls := "absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none",
              Icons.solid.search().amend(svg.cls := "text-gray-400")
            ),
            input(
              tpe := "search",
              name := "search",
              idAttr := "search",
              cls := "focus:ring-pink-500 focus:border-pink-500 block w-full pl-10 sm:text-sm border-gray-300 rounded-md",
              placeholder := "Hledat",
              composeEvents(onInput.mapToValue.setAsValue.map(SetFilter(_)))(
                _.throttle(500)
              ) --> actions
            )
          )
        ),
        button(
          tpe := "submit",
          cls := "inline-flex justify-center px-3.5 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-pink-500",
          Icons.solid.filter().amend(svg.cls := "text-gray-400"),
          span(
            cls := "sr-only",
            "Hledat"
          )
        )
      )
    )
