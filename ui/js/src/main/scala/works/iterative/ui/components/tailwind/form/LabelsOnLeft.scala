package works.iterative
package ui.components.tailwind.form

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*

object LabelsOnLeft:

    def fields(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
        div(
            cls := "mt-6 sm:mt-5 space-y-6 sm:space-y-5",
            mods
        )

    def header(header: String, description: String): HtmlElement =
        div(
            h3(cls := "text-lg leading-6 font-medium text-gray-900", header),
            p(cls := "mt-1 max-w-2xl text-sm text-gray-500", description)
        )

    def section(header: HtmlElement, rows: HtmlElement*): HtmlElement =
        div(
            cls := "space-y-6 sm:space-y-5",
            header,
            rows
        )

    def body(sections: HtmlElement*): HtmlElement =
        div(
            cls := "space-y-8 divide-y divide-gray-200 sm:space-y-5",
            sections
        )

    def form(body: HtmlElement, buttons: HtmlElement): HtmlElement =
        L.form(
            cls := "space-y-8 divide-y divide-gray-200",
            body,
            buttons
        )
end LabelsOnLeft
