package works.iterative.ui.components.tailwind
package laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.core.UserMessage

object LaminarExtensions:
  inline given userMessageToModifier(using
      ctx: ComponentContext[_]
  ): Conversion[UserMessage, Modifier[HtmlElement]] with
    inline def apply(msg: UserMessage) = ctx.messages(msg)

  inline given userMessageToString(using
      ctx: ComponentContext[_]
  ): Conversion[UserMessage, String] with
    inline def apply(msg: UserMessage) = ctx.messages(msg)
