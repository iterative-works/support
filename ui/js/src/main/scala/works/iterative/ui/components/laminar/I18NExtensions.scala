package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.*
import works.iterative.core.{MessageId, UserMessage}
import org.scalajs.dom
import works.iterative.core.MessageCatalogue
import works.iterative.ui.components.ComponentContext
import scalajs.js

trait I18NExtensions:
    given messageCatalogueFromContext(using
        ctx: ComponentContext[?]
    ): MessageCatalogue =
        ctx.messages

    extension (msg: UserMessage)
        inline def asElement(using MessageCatalogue): HtmlElement =
            span(msg.asMod)

        inline def asOptionalElement(using
            messages: MessageCatalogue
        ): Option[HtmlElement] =
            messages.get(msg).map(t => span(msgAttrs(msg.id, t)))

        inline def asMod(using messages: MessageCatalogue): Mod[HtmlElement] =
            msgAttrs(msg.id, messages(msg))

        private inline def msgAttrs(id: MessageId, text: String)(using
            messages: MessageCatalogue
        ): HtmlMod =
            modSeq(
                dataAttr("msgid")(id.toString()),
                dataAttr("msgprefix")(messages.currentPrefixes.mkString(",")),
                text
            )
    end extension

    given (using MessageCatalogue): HtmlRenderable[UserMessage] with
        def toHtml(msg: UserMessage): Modifier[HtmlElement] =
            msg.asElement

    extension (msgId: MessageId)
        def node: Node = I18NExtensions.messageNode(msgId)

    extension (userMessage: UserMessage)
        def node: Node = I18NExtensions.messageNode(userMessage)
end I18NExtensions

object I18NExtensions:
    // Inspired by how frontroute handles LocationState
    // The message catalogue is stored on the element
    // And when looking for a message, it traverses the parents
    // to find the catalogue and translate
    // Also it is possible to nest the prefixes in this way
    // scalafix:off DisableSyntax.var, DisableSyntax.null
    // JS interop: DOM element storage and traversal requires mutable state and null checks
    @js.native
    private trait ElementWithMessages extends js.Any:
        var ____messages: js.UndefOr[MessageCatalogue]

    def initContext(messages: MessageCatalogue): HtmlMod =
        onMountCallback: ctx =>
            ctx.thisNode.ref.asInstanceOf[ElementWithMessages].____messages = messages

    def nestContext(context: String): HtmlMod =
        onMountCallback: ctx =>
            closestMessages(ctx.thisNode.ref).foreach: msgs =>
                ctx.thisNode.ref.asInstanceOf[ElementWithMessages].____messages =
                    msgs.nested(context)

    private def closestMessages(node: dom.Node): Option[MessageCatalogue] =
        val m = node.asInstanceOf[ElementWithMessages]
        if m.____messages.isEmpty then
            if node.parentNode != null then closestMessages(node.parentNode)
            else None
        else Some(m.____messages.get)
        end if
    end closestMessages
    // scalafix:on DisableSyntax.var, DisableSyntax.null

    def withMessages(messages: MessageCatalogue)(mods: HtmlMod*): Div =
        div(
            cls("contents"),
            initContext(messages),
            mods
        )

    def inMessageContext(context: MessageId)(mods: HtmlMod*): Div =
        div(cls("contents"), nestContext(context.value), mods)

    def messageNode(key: UserMessage): Node =
        val translation = Var[String]("")
        span(
            cls("contents"),
            child.text <-- translation.signal,
            onMountCallback: ctx =>
                closestMessages(ctx.thisNode.ref).foreach: msgs =>
                    msgs.get(key).foreach(translation.set(_))
        )
    end messageNode
end I18NExtensions
