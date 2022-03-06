package fiftyforms.ui.components.tailwind
package form

import com.raquo.laminar.api.L.{*, given}
import io.laminext.syntax.core.*

case class ComboBox(
    id: String,
    options: Signal[List[ComboBox.Option]],
    valueUpdates: Observer[List[String]]
)

object ComboBox:

  extension (m: ComboBox)
    def toHtml: HtmlElement =
      val isOpen = Var(false)
      div(
        cls := "relative mt-1",
        input(
          idAttr := m.id,
          tpe := "text",
          cls := "w-full rounded-md border border-gray-300 bg-white py-2 pl-3 pr-12 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 sm:text-sm",
          role := "combobox",
          aria.controls := "options",
          aria.expanded := false,
          onClick.mapTo(true) --> isOpen.writer
        ),
        button(
          tpe := "button",
          cls := "absolute inset-y-0 right-0 flex items-center rounded-r-md px-2 focus:outline-none", {
            import svg.*
            svg(
              cls := "h-5 w-5 text-gray-400",
              xmlns := "http://www.w3.org/2000/svg",
              viewBox := "0 0 20 20",
              fill := "currentColor",
              CustomAttrs.svg.ariaHidden := true,
              path(
                fillRule := "evenodd",
                d := "M10 3a1 1 0 01.707.293l3 3a1 1 0 01-1.414 1.414L10 5.414 7.707 7.707a1 1 0 01-1.414-1.414l3-3A1 1 0 0110 3zm-3.707 9.293a1 1 0 011.414 0L10 14.586l2.293-2.293a1 1 0 011.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z",
                clipRule := "evenodd"
              )
            )
          }
        ),
        ul(
          cls <-- isOpen.signal.switch("", "hidden"),
          cls := "absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm",
          idAttr := "options",
          role := "listbox",
          children <-- m.options.map(_.map(_.toHtml))
        )
      )

  case class Option(value: String, active: Boolean)

  object Option:
    extension (m: Option)
      def toHtml: HtmlElement =
        li(
          cls := "relative cursor-default select-none py-2 pl-8 pr-4",
          cls := (if m.active then "text-white bg-indigo-600"
                  else "text-gray-900"),
          idAttr := "option-0",
          role := "option",
          tabIndex := -1,
          span(
            cls := "block truncate",
            m.value
          ),
          if m.active then
            span(
              cls := "absolute inset-y-0 left-0 flex items-center pl-1.5",
              cls := (if m.active then "text-white" else "text-indigo-600"), {
                import svg.*
                svg(
                  cls := "h-5 w-5",
                  xmlns := "http://www.w3.org/2000/svg",
                  viewBox := "0 0 20 20",
                  fill := "currentColor",
                  CustomAttrs.svg.ariaHidden := true,
                  path(
                    fillRule := "evenodd",
                    d := "M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z",
                    clipRule := "evenodd"
                  )
                )
              }
            )
          else emptyNode
        )
