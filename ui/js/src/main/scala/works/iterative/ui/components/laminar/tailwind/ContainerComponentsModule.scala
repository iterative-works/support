package works.iterative.ui.components.laminar.tailwind.ui

import com.raquo.laminar.api.L.*

trait ContainerComponentsModule:
  object container:
    /** Full-width on mobile, constrained with padded content above */
    def default(content: HtmlMod*): HtmlElement =
      div(cls("mx-auto max-w-7xl sm:px-6 lg:px-8"), content)

    def padded(content: HtmlMod*): HtmlElement =
      default(cls("px-4"), content)

    def narrow(content: HtmlMod*): HtmlElement =
      padded(
        div(cls("mx-auto max-w-3xl"), content)
      )
