package works.iterative.ui.components.laminar.tailwind.ui

import com.raquo.laminar.api.L.*
import works.iterative.ui.components.laminar.LaminarExtensions.*
import works.iterative.ui.components.ComponentContext
import works.iterative.core.UserMessage
import works.iterative.core.MessageId

trait ZeroComponentsModule:
  object zero:
    def message(id: MessageId)(using ComponentContext[?]): HtmlElement =
      message(UserMessage(id), UserMessage(s"${id}.description"))

    def message(headerMessage: UserMessage, descriptionElement: HtmlMod)(using
        ComponentContext[?]
    ): HtmlElement =
      message(headerMessage.asElement, descriptionElement)

    def message(headerMessage: UserMessage, descriptionMessage: UserMessage)(
        using ComponentContext[?]
    ): HtmlElement =
      message(
        headerMessage.asElement,
        p(cls("mt-1 text-sm text-gray-500"), descriptionMessage.asElement)
      )

    def message(
        headerElement: HtmlMod,
        descriptionElement: HtmlMod
    ): HtmlElement =
      div(
        cls("text-center"),
        h3(
          cls("mt-2 text-sm font-semibold text-gray-900"),
          headerElement
        ),
        descriptionElement
      )
