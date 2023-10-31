package works.iterative.ui.components.laminar.tailwind.ui

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*

import io.laminext.syntax.core.*

trait ComboboxModule:
  self: IconsModule =>

  object combobox:
    object simple:
      def container(mods: HtmlMod*): Div = div(cls("relative mt-2"), mods)

      def button(mods: HtmlMod*): Button =
        L.button(
          cls(
            "absolute inset-y-0 right-0 flex items-center rounded-r-md px-2 focus:outline-none"
          ),
          tpe("button"),
          icons.chevronUpDown(svg.cls("h-5 w-5 text-gray-400")),
          mods
        )

      def options(mods: HtmlMod*) =
        ul(
          cls(
            "absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm"
          ),
          role := "listbox",
          mods
        )

      def option(isActive: Signal[Boolean], isSelected: Signal[Boolean])(
          mods: HtmlMod*
      ) =
        li(
          cls(
            "relative cursor-default select-none py-2 pl-3 pr-9 text-gray-900"
          ),
          cls.toggle("text-white bg-indigo-600") <-- isActive,
          cls.toggle("text-gray-900") <-- isActive.not,
          cls.toggle("font-semibold") <-- isSelected,
          mods
        )

      def optionValue(l: String): HtmlElement =
        span(cls("block truncate"), l)

      def checkmark(isActive: Signal[Boolean]): HtmlElement =
        span(
          cls("absolute inset-y-0 right-0 flex items-center pr-4"),
          cls.toggle("text-indigo-600") <-- isActive.not,
          cls.toggle("text-white") <-- isActive,
          icons.check(svg.cls("h-5 w-5"))
        )
