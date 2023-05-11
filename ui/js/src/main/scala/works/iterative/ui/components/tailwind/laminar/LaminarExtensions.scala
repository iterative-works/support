package works.iterative.ui.components.tailwind
package laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.core.UserMessage
import io.laminext.syntax.core.*

object LaminarExtensions:
  extension (msg: UserMessage)
    inline def asElement(using ctx: ComponentContext[_]): HtmlElement =
      span(msg.asMod)

    inline def asString(using ctx: ComponentContext[_]): String =
      ctx.messages(msg)

    inline def asMod(using ctx: ComponentContext[_]): Mod[HtmlElement] =
      nodeSeq(dataAttr("msgid")(msg.id.toString()), ctx.messages(msg))

  given (using ComponentContext[_]): HtmlRenderable[UserMessage] with
    def toHtml(msg: UserMessage): Modifier[HtmlElement] =
      msg.asElement
