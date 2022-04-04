package works.iterative
package ui.components.headless

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement

trait ItemContainer[A]:
  def contramap[B](f: B => A): ItemContainer[B]
  def map(f: A => HtmlElement => HtmlElement): ItemContainer[A]

final case class Items[A](
    frame: Seq[HtmlElement] => HtmlElement,
    renderItem: A => HtmlElement
) extends ItemContainer[A]:
  def apply(items: Seq[A]): HtmlElement = frame(
    items.map(a => li(renderItem(a)))
  )
  def contramap[B](f: B => A): Items[B] =
    Items(frame, b => renderItem(f(b)))
  def map(f: A => HtmlElement => HtmlElement): Items[A] =
    Items(frame, a => f(a)(renderItem(a)))

final case class GroupedItems[Key, A](
    frame: Seq[HtmlElement] => HtmlElement,
    renderItems: Key => Items[A]
) extends ItemContainer[A]:
  def apply(items: Seq[(Key, Seq[A])]): HtmlElement = frame(
    items.map(renderItems(_)(_))
  )
  def contramap[B](f: B => A): GroupedItems[Key, B] =
    GroupedItems(frame, k => renderItems(k).contramap(f))
  def map(f: A => HtmlElement => HtmlElement): GroupedItems[Key, A] =
    GroupedItems(frame, k => renderItems(k).map(f))
