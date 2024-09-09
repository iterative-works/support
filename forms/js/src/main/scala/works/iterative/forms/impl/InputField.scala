package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import works.iterative.ui.model.forms.IdPath

@deprecated(message = "replace with Components.inputField")
object InputField:
    def apply(id: IdPath, inError: Signal[Boolean])(mods: HtmlMod*) = input(
        cls(
            "text-red-900 ring-red-300 placeholder:text-red-300 focus:ring-red-500"
        ) <-- inError,
        cls(
            "text-gray-900 ring-gray-300 placeholder:text-gray-400 focus:ring-indigo-600"
        ) <-- inError.not,
        cls(
            "block w-full rounded-md border-0 py-1.5 shadow-sm ring-1 ring-inset focus:ring-2 focus:ring-inset sm:text-sm sm:leading-6"
        ),
        idAttr(id.toHtmlId),
        nameAttr(id.toHtmlName),
        mods
    )
end InputField
