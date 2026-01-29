package works.iterative
package ui.components.tailwind

import com.raquo.laminar.api.L.*

case class CardWithHeader(
    title: String,
    actions: Modifier[HtmlElement],
    content: Modifier[HtmlElement]
):
    def element: HtmlElement =
        div(
            cls("bg-white shadow overflow-hidden sm:rounded-md"),
            div(
                cls := "bg-white px-4 py-5 border-b border-gray-200 sm:px-6",
                div(
                    cls := "-ml-4 -mt-2 flex items-center justify-between flex-wrap sm:flex-nowrap",
                    div(
                        cls := "ml-4 mt-2",
                        h3(
                            cls := "text-lg leading-6 font-medium text-gray-900",
                            title
                        )
                    ),
                    div(
                        cls := "ml-4 mt-2 flex-shrink-0",
                        actions
                    )
                )
            ),
            content
        )
end CardWithHeader
