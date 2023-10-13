package works.iterative.ui
package laminar
package headless

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import org.scalajs.dom

/** Headless menu.
  *
  * The menu wraps existing elements to add the behavior.
  *
  * It expects menu button, that will toggle the menu state, and show menu
  * items.
  */
object Menu:

  /** Global menu context */
  class Ctx:
    private[Menu] val open: Var[Boolean] = Var(false)

    val isOpen: Signal[Boolean] = open.signal
    val close: Observer[Any] = open.writer.contramap(_ => false)

  class ItemCtx(val disabled: Boolean)(using ctx: Ctx):
    private[Menu] val active: Var[Boolean] = Var(false)
    val isActive: Signal[Boolean] = active.signal
    export ctx.close

  type Render = Ctx ?=> HtmlElement
  type ItemRender = ItemCtx ?=> HtmlElement

  def apply(
      content: Render
  ): HtmlElement =
    given ctx: Ctx = Ctx()
    content.amendThis(n =>
      windowEvents(_.onClick).filterWith(ctx.isOpen).map(_.target).collectOpt {
        case el: org.scalajs.dom.HTMLElement if !n.ref.contains(el) =>
          Some(false)
      } --> ctx.open.writer
    )

  def button(but: Render)(using ctx: Ctx): HtmlElement =
    but.amend(
      aria.expanded <-- ctx.isOpen,
      onClick.mapTo(true) --> ctx.open.writer
    )

  def items(el: Render)(using ctx: Ctx): HtmlElement =
    el.amend(cls.toggle("hidden") <-- ctx.isOpen.not)

  def item(it: ItemRender, disabled: Boolean = false)(using
      ctx: Ctx
  ): HtmlElement =
    given ictx: ItemCtx = ItemCtx(disabled)

    it.amend(
      onMouseEnter.mapTo(true) --> ictx.active.writer,
      onMouseLeave.mapTo(false) --> ictx.active.writer,
      onClick.mapTo(false) --> ctx.open.writer
    )
