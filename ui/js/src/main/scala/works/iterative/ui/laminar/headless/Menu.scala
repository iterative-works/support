package works.iterative.ui
package laminar
package headless

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*

/** Headless menu.
  *
  * The menu wraps existing elements to add the behavior.
  *
  * It expects menu button, that will toggle the menu state, and show menu
  * items.
  *
  * Inspired by headlessui components, but lacks most of the functionality,
  * especially the ARIA stuff.
  */
// TODO: keyboard support, aria elements, disabled items
object Menu:

  /** Global menu context */
  class Ctx:
    private[Menu] val open: Var[Boolean] = Var(false)

    val isOpen: Signal[Boolean] = open.signal
    val close: Observer[Any] = open.writer.contramap(_ => false)

  class ItemCtx(using ctx: Ctx):
    private[Menu] val active: Var[Boolean] = Var(false)
    val isActive: Signal[Boolean] = active.signal
    export ctx.close

  type Render = Ctx ?=> HtmlElement
  type ItemRender = ItemCtx ?=> HtmlElement

  def apply(
      content: Render
  ): HtmlElement =
    given ctx: Ctx = Ctx()
    content.amend(closeOnClickOutside(ctx.open))

  def button(but: Render)(using ctx: Ctx): HtmlElement =
    but.amend(
      aria.expanded <-- ctx.isOpen,
      dataState <-- ctx.isOpen.map {
        case true  => "open"
        case false => ""
      },
      onClick.mapTo(true) --> ctx.open.writer
    )

  def items(el: Render)(using ctx: Ctx): HtmlElement =
    el.amend(
      cls.toggle("hidden") <-- ctx.isOpen.not,
      dataState <-- ctx.isOpen.map {
        case true  => "open"
        case false => ""
      }
    )

  def item(it: ItemRender)(using
      ctx: Ctx
  ): HtmlElement =
    given ictx: ItemCtx = ItemCtx()

    it.amend(
      onMouseEnter.mapTo(true) --> ictx.active.writer,
      onMouseLeave.mapTo(false) --> ictx.active.writer,
      onClick.mapTo(false) --> ctx.open.writer,
      dataState <-- ictx.isActive.map {
        case true  => "active"
        case false => ""
      }
    )
