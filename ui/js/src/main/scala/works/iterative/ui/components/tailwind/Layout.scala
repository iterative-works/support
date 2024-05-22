package works.iterative
package ui.components
package tailwind

import com.raquo.laminar.api.L.*

object Layout:
    def card(content: Modifier[HtmlElement]*)(using
        cctx: ComponentContext[?]
    ): Div =
        div(cls("bg-white shadow sm:rounded-md overflow-hidden"), content)
end Layout
