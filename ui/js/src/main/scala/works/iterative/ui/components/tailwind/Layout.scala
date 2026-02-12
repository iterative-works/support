package works.iterative
package ui.components
package tailwind

import com.raquo.laminar.api.L.*

object Layout:
    @scala.annotation.nowarn("msg=unused implicit parameter")
    def card(content: Modifier[HtmlElement]*)(using
        ComponentContext[?]
    ): Div =
        div(cls("bg-white shadow sm:rounded-md overflow-hidden"), content)
end Layout
