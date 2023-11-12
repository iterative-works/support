package works.iterative.ui.laminar.headless

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*

/** Headless combobox with autocomplete.
  *
  * The combobox wraps existing elements to add the behavior.
  *
  * Inspired by headlessui components, but lacks most of the functionality,
  * especially the ARIA stuff.
  */
// TODO: aria elements, disabled items, data attributes
object Combobox:
  class Ctx[T](val initialValue: Option[T]):
    private[Combobox] val valueVar: Var[Option[T]] = Var(initialValue)
    private[Combobox] val inputVar: Var[String] = Var("")
    private[Combobox] val open: Var[Boolean] = Var(false)
    private[Combobox] val isFocused: Var[Boolean] = Var(false)
    private[Combobox] val isChanged: Var[Boolean] = Var(false)
    private[Combobox] val isEmpty: Signal[Boolean] =
      valueVar.signal.map(_.isEmpty)
    private[Combobox] val itemsVar: Var[Seq[T]] = Var[Seq[T]](Nil)
    private[Combobox] val activeVar: Var[Set[T]] = Var[Set[T]](Set.empty)
    val itemsWriter: Observer[Seq[T]] = itemsVar.writer
    val valueWriter: Observer[Option[T]] = valueVar.writer
    val items: Signal[Seq[T]] = itemsVar.signal
    val value: Signal[Option[T]] = valueVar.signal.distinct
    val isOpen: Signal[Boolean] = open.signal
    val query: Signal[String] = inputVar.signal

  class ItemCtx[T](val value: T)(using ctx: Ctx[T]):
    private[Combobox] val active: Var[Boolean] = Var(false)
    val isActive: Signal[Boolean] = ctx.activeVar.signal.map(_.contains(value))
    val isSelected: Signal[Boolean] = ctx.value.map(_.contains(value))
    export ctx.isOpen

  type RenderInput[T] = Ctx[T] ?=> Input
  type Render[T] = Ctx[T] ?=> HtmlElement
  type ItemRender[T] = ItemCtx[T] ?=> T => HtmlElement

  def ctx[T](using c: Ctx[T]): Ctx[T] = c

  def ictx[T](using c: ItemCtx[T]): ItemCtx[T] = c

  def apply[T](initialValue: Option[T] = None)(
      content: Render[T]
  ): HtmlElement =
    given ctx: Ctx[T] = Ctx(initialValue)
    content.amend(closeOnClickOutside(ctx.open))

  def input[T](using
      ctx: Ctx[T]
  )(displayValue: T => String)(
      inp: RenderInput[T]
  ): Input =
    val currentValue = ctx.value.map(_.map(displayValue).getOrElse(""))
    inp.amend(
      currentValue --> ctx.inputVar.writer,
      onFocus.mapTo(true) --> ctx.isFocused.writer,
      onBlur.mapTo(false) --> ctx.isFocused.writer,
      ctx.inputVar.signal
        .combineWithFn(currentValue)(_ != _) --> ctx.isChanged.writer,
      // Reset the value if we get back to blank after having something set
      onInput.mapToValue
        .filter(_.isEmpty)
        .compose(
          _.filterWith(ctx.isChanged.signal).mapTo(None)
        ) --> ctx.valueVar.writer,
      ctx.isFocused.signal
        .combineWithFn(
          ctx.isChanged.signal,
          ctx.isEmpty
        )((f, e, c) => f && (e || c))
        .changes
        .collect { case true =>
          true
        } --> ctx.open.writer,
      // On close, set the value to the selected value
      ctx.isOpen.changes
        .filter(v => !v)
        .sample(ctx.value)
        .collectSome
        .map(displayValue) --> ctx.inputVar.writer,
      EventStream.fromSeq(
        ctx.initialValue.map(displayValue).toSeq
      ) --> ctx.inputVar.writer,
      // Control the input value from the inputVar
      controlled(
        value <-- ctx.inputVar.signal,
        onInput.mapToValue --> ctx.inputVar.writer
      ),
      // Close on escape
      onKeyDown
        .map(_.key)
        .collect { case "Escape" =>
          false
        }
        .preventDefault
        .stopPropagation --> ctx.open.writer,
      // On key up or down, open menu if closed
      onKeyDown
        .map(_.key)
        .collect { case "ArrowDown" | "ArrowUp" =>
          true
        }
        .preventDefault
        .stopPropagation
        .compose(
          _.filterWith(ctx.open.signal.not).mapTo(true)
        ) --> ctx.open.writer,
      // On key up or down, select the next or previous item
      onKeyDown
        .map(_.key)
        .collect {
          case "ArrowDown" => true
          case "ArrowUp"   => false
        }
        .preventDefault
        .stopPropagation
        .compose(
          _.withCurrentValueOf(ctx.items, ctx.activeVar.signal)
            .map { (arrow, items, active) =>
              def nextItem(it: Seq[T]) = it
                .dropWhile(!active.contains(_))
                .drop(1)
                .headOption
                .orElse(it.headOption)
              if arrow then nextItem(items) else nextItem(items.reverse)
            }
            .collectSome
            .map(Set(_))
        ) --> ctx.activeVar.writer,
      // On enter, choose active item
      onKeyDown
        .map(_.key)
        .collect { case "Enter" =>
          true
        }
        .preventDefault
        .stopPropagation
        .compose(
          _.sample(ctx.activeVar.signal).map(_.headOption).collectSome
        ) --> Observer.combine(
        ctx.valueVar.writer.contramapSome,
        ctx.open.writer.contramap(_ => false)
      )
    )

  def button[T](using ctx: Ctx[T])(but: Render[T]): HtmlElement =
    but.amend(
      // Toggle the open on click
      onClick.stopPropagation --> (_ => ctx.open.toggle())
    )

  def options[T](using
      ctx: Ctx[T]
  )(container: Render[T])(opt: ItemRender[T]): HtmlElement =
    container.amend(
      // Show if open
      cls.toggle("hidden") <-- (ctx.isOpen.not || ctx.items.map(_.isEmpty)),
      onClick.mapTo(false).stopPropagation --> ctx.open.writer,
      children <-- ctx.items.map(_.map(option(_)(opt))),
      ctx.isOpen.changes
        .filter(v => !v)
        .mapTo(Set.empty) --> ctx.activeVar.writer
    )

  def option[T](using ctx: Ctx[T])(value: T)(opt: ItemRender[T]): HtmlElement =
    given ictx: ItemCtx[T] = ItemCtx(value)
    opt(value).amend(
      // Set the value if clicked
      onClick.mapTo(value) --> ctx.valueVar.writer.contramapSome,
      onMouseEnter.mapTo(value) --> ctx.activeVar.updater[T](_ + _),
      onMouseLeave.mapTo(value) --> ctx.activeVar.updater[T](_ - _)
    )
