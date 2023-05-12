package works.iterative.ui.components.tailwind
package laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.core.UserMessage
import io.laminext.syntax.core.*
import works.iterative.core.MessageId

object LaminarExtensions:
  extension (msg: UserMessage)
    inline def asElement(using ctx: ComponentContext[_]): HtmlElement =
      span(msg.asMod)

    inline def asOptionalElement(using
        ctx: ComponentContext[_]
    ): Option[HtmlElement] =
      ctx.messages.get(msg).map(t => span(msgAttrs(msg.id, t)))

    inline def asString(using ctx: ComponentContext[_]): String =
      ctx.messages(msg)

    inline def asMod(using ctx: ComponentContext[_]): Mod[HtmlElement] =
      msgAttrs(msg.id, ctx.messages(msg))

    private inline def msgAttrs(id: MessageId, text: String): HtmlMod =
      nodeSeq(dataAttr("msgid")(id.toString()), text)

  given (using ComponentContext[_]): HtmlRenderable[UserMessage] with
    def toHtml(msg: UserMessage): Modifier[HtmlElement] =
      msg.asElement
