package works.iterative.ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}

object Modal:
  def render(elem: HtmlElement, close: Modifier[HtmlElement]): HtmlElement =
    // This sequence tricks browser into displaying modal content centered
    // Inspired by modal in headless ui playground
    // https://github.com/tailwindlabs/headlessui/blob/fdd26297953080d5ec905dda0bf5ec9607897d86/packages/playground-react/pages/transitions/component-examples/modal.tsx#L78-L79
    inline def browserCenteringModalTrick: Modifier[HtmlElement] =
      Seq[Modifier[HtmlElement]](
        span(cls("hidden sm:inline-block sm:h-screen sm:align-middle")),
        "​" // Zero width space
      )

    inline def overlay: Modifier[HtmlElement] =
      // Page overlay
      /* TODO: transition
                enter="ease-out duration-300"
                enterFrom="opacity-0"
                enterTo="opacity-100"
                leave="ease-in duration-200"
                leaveFrom="opacity-100"
                leaveTo="opacity-0"
       */
      div(
        div(
          cls("fixed inset-0 transition-opacity"),
          div(cls("absolute inset-0 bg-gray-500 opacity-75")),
          close
        )
      )

    div(
      cls("fixed inset-0 z-20 overflow-y-auto"),
      div(
        cls("text-center sm:block sm:p-0"),
        overlay,
        browserCenteringModalTrick,
        div(
          cls(
            "inline-block transform overflow-hidden rounded-lg bg-white text-left align-bottom shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-7xl sm:align-middle"
          ),
          elem
        )
      )
    )
