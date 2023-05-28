package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.core.UserMessage
import io.laminext.syntax.core.*
import works.iterative.core.MessageId
import com.raquo.laminar.modifiers.RenderableNode
import com.raquo.laminar.nodes.ChildNode.Base
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.components.laminar.HtmlRenderable
import com.raquo.airstream.custom.CustomStreamSource
import com.raquo.airstream.custom.CustomSource
import zio.IsSubtypeOfError

object LaminarExtensions extends I18NExtensions with ZIOInteropExtensions

trait I18NExtensions:
  extension (msg: UserMessage)
    inline def asElement(using ctx: ComponentContext[?]): HtmlElement =
      span(msg.asMod)

    inline def asOptionalElement(using
        ctx: ComponentContext[?]
    ): Option[HtmlElement] =
      ctx.messages.get(msg).map(t => span(msgAttrs(msg.id, t)))

    inline def asString(using ctx: ComponentContext[?]): String =
      ctx.messages(msg)

    inline def asOptionalString(using
        ctx: ComponentContext[?]
    ): Option[String] =
      ctx.messages.get(msg)

    inline def asMod(using ctx: ComponentContext[?]): Mod[HtmlElement] =
      msgAttrs(msg.id, ctx.messages(msg))

    private inline def msgAttrs(id: MessageId, text: String): HtmlMod =
      nodeSeq(dataAttr("msgid")(id.toString()), text)

  given (using ComponentContext[?]): HtmlRenderable[UserMessage] with
    def toHtml(msg: UserMessage): Modifier[HtmlElement] =
      msg.asElement

trait ZIOInteropExtensions:
  import zio.{ZIO, Runtime, Unsafe, Fiber, Cause}

  extension [R, E, O](effect: ZIO[R, E, O])
    def toEventStream(using runtime: Runtime[R])(using
        ev: E IsSubtypeOfError Throwable
    ): EventStream[O] =
      var fiberRuntime: Fiber.Runtime[E, O] = null
      EventStream.fromCustomSource(
        start = (fireValue, fireError, getStartIndex, getIsStarted) =>
          Unsafe.unsafe { implicit unsafe =>
            fiberRuntime = runtime.unsafe.fork(effect)
            fiberRuntime.unsafe.addObserver(exit =>
              exit.foldExit(cause => fireError(cause.squash), fireValue)
            )
          },
        stop = _ =>
          if fiberRuntime != null then
            Unsafe.unsafe { implicit unsafe =>
              runtime.unsafe.run(fiberRuntime.interrupt).ignore
            }
            fiberRuntime = null
          else ()
      )

    def toEventStreamWith(mapError: E => Throwable)(using
        runtime: Runtime[R]
    ): EventStream[O] =
      var fiberRuntime: Fiber.Runtime[E, O] = null
      EventStream.fromCustomSource(
        start = (fireValue, fireError, getStartIndex, getIsStarted) =>
          Unsafe.unsafe { implicit unsafe =>
            fiberRuntime = runtime.unsafe.fork(effect)
            fiberRuntime.unsafe.addObserver(exit =>
              exit.foldExit(
                cause => fireError(cause.squashWith(mapError)),
                fireValue
              )
            )
          },
        stop = _ =>
          if fiberRuntime != null then
            Unsafe.unsafe { implicit unsafe =>
              runtime.unsafe.run(fiberRuntime.interrupt).ignore
            }
            fiberRuntime = null
          else ()
      )
